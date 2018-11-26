package radim.outfit

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.content.Intent
import android.os.Environment
import android.util.Log
import locus.api.android.utils.LocusUtils
import locus.api.android.utils.exceptions.RequiredVersionMissingException
import locus.api.objects.extra.Track
import net.rdrei.android.dirchooser.DirectoryChooserActivity
import net.rdrei.android.dirchooser.DirectoryChooserConfig
import java.io.File
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.widget.ProgressBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_export.*
import kotlinx.android.synthetic.main.content_path.*
import locus.api.android.utils.LocusInfo
import radim.outfit.core.Stats
import radim.outfit.core.export.logic.*
import locus.api.android.ActionTools
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import radim.outfit.core.FilenameCharsFilter
import radim.outfit.core.getFilename
import radim.outfit.debugdumps.writeTextFile

const val LOG_TAG = "MAIN"
const val REQUEST_CODE_OPEN_DIRECTORY = 9999
const val REQUEST_CODE_PERM_WRITE_EXTERNAL = 7777
const val EXTRA_MESSAGE_FINISH = "start finish gracefully activity to explain what happened"

// error codes:
// 1 - 7

fun AppCompatActivity.getString(name: String): String {
    return resources.getString(resources.getIdentifier(name, "string", packageName))
}

class MainActivity : AppCompatActivity() {

    private val debug = true

    private val exportListener = ExportListener(
            ExportFunction(),
            ExportPOJO(null, null, null),
            ::exportListenerCallback,
            ::clickedCallback
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar.visibility = ProgressBar.INVISIBLE

        val activeLocus = LocusUtils.getActiveVersion(this)
        if (activeLocus == null) {
            failGracefully(this.getString("locus_not_installed") + " Error 1")
            return
        }

        try {
            ActionTools.getLocusInfo(this, activeLocus)
        } catch (e: RequiredVersionMissingException) {
            failGracefully(this.getString("required_version_missing - ") + e.localizedMessage + " Error 2")
            return
        }

        if (!isExternalStorageWritable()) {
            failGracefully("isExternalStorageWritable() == false" + " Error 3")
            return
        }

        etFilename.filters = arrayOf(FilenameCharsFilter())
        btnExport.setOnClickListener(exportListener)
        exportListener.attachView(etFilename)
        exportListener.attachDefaultFilename(this.getString("default_filename"))

        if (permWriteIsGranted()) {
            setAppStorageRoot()
            setTvRootDir()
        } else {
            requestPermWrite()
        }

        Log.i(LOG_TAG, "activeLocus.versionName: ${activeLocus.versionName}")
        Log.i(LOG_TAG, "activeLocus.versionCode: ${activeLocus.versionCode}")
        val info = locusInfo()
        if (!info.isRunning) toast(getString("locus_not_running"), Toast.LENGTH_SHORT)

        if (LocusUtils.isIntentTrackTools(this.intent)) {
            // event performed if user tap on your app icon in tools menu of 'Track'
            handleIntentTrackToolsMenu(this, this.intent)
        }
    }

    // isIntentTrackTools(intent) = true
    private fun handleIntentTrackToolsMenu(act: Activity, intent: Intent) {
        val track: Track?
        try {
            track = LocusUtils.handleIntentTrackTools(act, intent)
        } catch (e: RequiredVersionMissingException) {
            failGracefully(this.getString("required_version_missing - ") + e.localizedMessage + " Error 4")
            return
        } catch (e: Exception) {
            failGracefully(e.localizedMessage + " Error 5")
            return
        }
        if (track != null && track.points != null && track.points.size > 0) {
            // do work
            tvStats.text = Stats().basicInfo(track, this)
            val filename = getFilename(track.name, getString("default_filename"))
            etFilename.setText(filename)
            setTrack(track, exportListener)
            setFilename(filename, exportListener)
        } else {
            failGracefully(" Error 6")
            return
        }
    }

    //  CALLBACKS START

    private fun exportListenerCallback(result: Result) {
        // enable executive UI
        btnExport.isEnabled = true
        progressBar.visibility = ProgressBar.INVISIBLE

        if (debug) {
            //fire and forget writing log file
            doAsync {
                var savedOK = true
                try {
                    when (result) {
                        is Result.Success -> {
                            writeTextFile(File(result.logFileDir.absolutePath +
                                    File.separatorChar +
                                    result.filename +
                                    ".debug.dump"
                            ), result.debugMessage)
                        }
                        is Result.Fail -> {
                            if (result.logFileDir != null && result.logFileDir.exists() && result.filename != null) {
                                val rootPath = result.logFileDir.absolutePath + File.separatorChar + result.filename
                                writeTextFile(File("$rootPath.debug.dump"), result.debugMessage)
                                writeTextFile(File("$rootPath.error.dump"), result.errorMessage)
                            } else {
                                savedOK = false
                                Log.e(LOG_TAG, result.errorMessage.toString())
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    savedOK = false
                }
                uiThread {
                    if (savedOK) toast(getString("debug_log_written"), Toast.LENGTH_SHORT)
                    else toast(getString("debug_log_write_error"), Toast.LENGTH_SHORT)
                    if(result is Result.Fail) failGracefully(result.errorMessage.toString())
                }
            }
        }
        // TODO do something public about result
    }

    private fun clickedCallback() {
        // disable executive UI
        btnExport.isEnabled = false
        progressBar.visibility = ProgressBar.VISIBLE
    }

    //  CALLBACKS END

    fun directoryPick(@Suppress("UNUSED_PARAMETER") v: View) {
        if (!permWriteIsGranted()) toast(getString("permission_needed"), Toast.LENGTH_SHORT)
        val rootPath = getRoot(exportListener)?.path
                ?: Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOCUMENTS).path
        val chooserIntent = Intent(this, DirectoryChooserActivity::class.java)
        Log.i(LOG_TAG, "calling DirectoryChooserActivity with root: $rootPath")

        val config = DirectoryChooserConfig.builder()
                .newDirectoryName("${getString(R.string.app_name)}${getString(R.string.new_directory_name)}")
                .allowReadOnlyDirectory(false)
                .allowNewDirectoryNameModification(true)
                .initialDirectory(rootPath)
                .build()

        chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config)
        startActivityForResult(chooserIntent, REQUEST_CODE_OPEN_DIRECTORY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY) {
            if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
                try {
                    handleDirectoryChoice(data!!
                            .getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR))
                } catch (e: Exception) {
                    failGracefully(" Error 7")
                    return
                }
            } else {
                Log.w(LOG_TAG, "Nothing selected")
            }
        }
    }

    private fun handleDirectoryChoice(selectedDir: String) {
        Log.i(LOG_TAG, "SELECTED_DIR: $selectedDir")
        val newRoot: File? = File(selectedDir)
        if (storageDirExists(newRoot)) {
            setRoot(newRoot, exportListener)
            setTvRootDir()
            Log.i(LOG_TAG, "new root: ${getRoot(exportListener)}")
        } else {
            setTvRootDir()
            Log.e(LOG_TAG, "storageDirExists(newRoot) == false - should never happen")
        }
    }

    private fun setTvRootDir() {
        val text: String? = getRoot(exportListener)?.path
        if (text != null) {
            tvRootDir.setTextColor(this.getColor(R.color.imitateButtons))
            tvRootDir.text = text
        } else {
            tvRootDir.setTextColor(this.getColor(R.color.colorAccent))
            tvRootDir.text = this.getString("not_set")
        }
    }

    // LOCUS INFO UTILS
    @Throws(RequiredVersionMissingException::class)
    private fun locusInfo(): LocusInfo {
        lateinit var info: LocusInfo
        try {
            info = ActionTools.getLocusInfo(this, LocusUtils.getActiveVersion(this))
        } catch (e: RequiredVersionMissingException) {
            e.printStackTrace()
        }
        return info
    }

    // EXTERNAL STORAGE UTILS

    /* Checks if external storage is available for read and write */
    private fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    private fun storageDirExists(file: File?): Boolean {
        return (file != null && file.isDirectory)
    }

    private fun setAppStorageRoot() {

        val locusExportDir = File(locusInfo().rootDirExport)
        if (storageDirExists(locusExportDir)) {
            setRoot(locusExportDir, exportListener)
        } else {
            val appExportRoot = File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS), this.getString("app_name"))
            val created: Boolean = appExportRoot.mkdirs()
            if (created) {
                Log.i(LOG_TAG, "setAppStorageRoot() - directory created")
            } else {
                Log.i(LOG_TAG, "setAppStorageRoot() - directory not created")
            }
            setRoot(appExportRoot, exportListener)
        }
    }

    // PERMISSION UTILS

    private fun permWriteIsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermWrite() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_PERM_WRITE_EXTERNAL)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_PERM_WRITE_EXTERNAL -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay!
                    setAppStorageRoot()
                    setTvRootDir()
                } else {
                    // permission denied, boo! Disable the
                    // functionality
                    setTvRootDir()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun toast(message: String, duration: Int) {
        val toast = Toast.makeText(applicationContext, message, duration)
        toast.show()
    }

    private fun getFinnishIntent(message: String): Intent {
        return Intent(this, FinishGracefully::class.java).apply {
            putExtra(EXTRA_MESSAGE_FINISH, message)
        }
    }

    private fun failGracefully(message: String) {
        val intent = getFinnishIntent(message)
        startActivity(intent)
        this.finish()
    }

}
