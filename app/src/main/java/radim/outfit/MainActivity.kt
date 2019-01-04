package radim.outfit

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.os.Bundle
import android.view.View
import android.content.Intent
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import locus.api.android.utils.LocusUtils
import locus.api.android.utils.exceptions.RequiredVersionMissingException
import locus.api.objects.extra.Track
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
import java.lang.RuntimeException

const val LOG_TAG_MAIN = "MAIN"
const val REQUEST_CODE_OPEN_DIRECTORY = 9999
const val REQUEST_CODE_PERM_WRITE_EXTERNAL = 7777
const val EXTRA_MESSAGE_FINISH = "start finish gracefully activity to explain what happened"
const val EXTRA_MESSAGE_VIEW_RESULTS = "start view results activity with ViewResultParcel extra"

// error codes:
// 1 - 7

fun AppCompatActivity.getString(name: String): String {
    return resources.getString(resources.getIdentifier(name, "string", packageName))
}

const val DEBUG_MODE = true

class MainActivity : AppCompatActivity(),
        OkActionProvider,
        LastSelectedValuesProvider,
        PermInfoProvider,
        Toaster {

    // https://drive.google.com/file/d/1wwYzoPQts1HreDpS614oMAVyafU07ZYF/view?usp=sharing
    private val exportListener = ExportListener(
            ExportFunction(),
            ExportPOJO(null, null, null),
            ::exportListenerCallback,
            ::disableExecutive,
            ::showSpeedPickerDialog,
            this,
            this,
            this
    )

    private fun showSpeedPickerDialog() {
        val fm = supportFragmentManager
        val spf = SpeedPickerFragment()
        spf.show(fm, "speed_picker_fragment")
    }

    private lateinit var sharedPreferences: SharedPreferences

    private val fail = FailGracefullyLauncher()

    // SpeedPickerFragment interfaces impl START
    override fun getOkAction(): (Float) -> Unit = exportListener.getOkAction()

    override fun getSpeed() = sharedPreferences.getInt(getString("last_seen_speed_value"), SPEED_DEFAULT)
    override fun getUnitsButtonId() = sharedPreferences.getInt(getString("last_seen_speed_units"), DEFAULT_UNITS_RADIO_BUTTON_ID)
    override fun setSpeed(value: Int) = persistInSharedPreferences(getString("last_seen_speed_value"), value)
    override fun setUnitsButtonId(id: Int) = persistInSharedPreferences(getString("last_seen_speed_units"), id)

    private fun <T> persistInSharedPreferences(key: String, value: T) {
        with(sharedPreferences.edit()) {
            when (value) {
                is Int -> {
                    putInt(key, value)
                }
                is Float -> {
                    putFloat(key, value)
                }
                is Boolean -> {
                    putBoolean(key, value)
                }
                else -> {
                    throw RuntimeException("unimplemented")
                }
            }
            apply()
        }
    }
    // SpeedPickerFragment interfaces impl END

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar.visibility = ProgressBar.INVISIBLE

        sharedPreferences = this.getSharedPreferences(
                getString(R.string.main_activity_preferences), Context.MODE_PRIVATE)

        with(sharedPreferences.edit()) {
            if (!sharedPreferences.contains(getString("last_seen_speed_value"))) {
                putInt(getString("last_seen_speed_value"), SPEED_DEFAULT)
            }
            if (!sharedPreferences.contains(getString("last_seen_speed_units"))) {
                putInt(getString("last_seen_speed_units"), DEFAULT_UNITS_RADIO_BUTTON_ID)
            }
            if (!sharedPreferences.contains(CIRC_BUFF_POINTER_KEY))
                initCircularBuffer(this)
            apply()
        }

        val activeLocus = LocusUtils.getActiveVersion(this)
        if (activeLocus == null) {
            fail.failGracefully(this, this.getString("locus_not_installed") + " Error 1")
            return
        }

        try {
            ActionTools.getLocusInfo(this, activeLocus)
        } catch (e: RequiredVersionMissingException) {
            fail.failGracefully(this, this.getString("required_version_missing - ") + e.localizedMessage + " Error 2")
            return
        }

        if (!isExternalStorageWritable()) {
            fail.failGracefully(this, "isExternalStorageWritable() == false" + " Error 3")
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

        Log.i(LOG_TAG_MAIN, "activeLocus.versionName: ${activeLocus.versionName}")
        Log.i(LOG_TAG_MAIN, "activeLocus.versionCode: ${activeLocus.versionCode}")
        val info = locusInfo()
        if (!info.isRunning) toast(getString("locus_not_running"), Toast.LENGTH_LONG)

        if (LocusUtils.isIntentTrackTools(this.intent)) {
            // event performed if user tap on your app icon in tools menu of 'Track'
            handleIntentTrackToolsMenu(this, this.intent)
        }
    }

    // isIntentTrackTools(intent) = true
    private fun handleIntentTrackToolsMenu(act: AppCompatActivity, intent: Intent) {
        disableExecutive()
        doAsync {
            var track: Track? = null
            try {
                track = LocusUtils.handleIntentTrackTools(act, intent)
                // or inject a mock
                //track = getTrackOkNoCP()
                //track = getTrackNullEndNoCP()
                //track = getTrackNullStartNoCP()
                //track = getTrackRandomNullsNoCP()
            } catch (e: RequiredVersionMissingException) {
                fail.failGracefully(act, act.getString("required_version_missing") + " " + e.localizedMessage + " Error 4")
            } catch (e: Exception) {
                fail.failGracefully(act, e.localizedMessage + " Error 5")
            }
            uiThread {
                if (track != null && track.points != null && track.points.size > 0) {
                    // do work
                    tvStats.text = Stats().basicInfo(track, act)
                    val filename = getFilename(track.name, getString("default_filename"))
                    etFilename.setText(filename)
                    setTrack(track, exportListener)
                    setFilename(filename, exportListener)
                    enableExecutive()
                } else {
                    fail.failGracefully(act, " null - Error 6")
                }
            }
        }
    }

    //  CALLBACKS START

    private fun exportListenerCallback(result: Result) {
        // enable executive UI
        btnExport.isEnabled = true
        progressBar.visibility = ProgressBar.INVISIBLE

        if (DEBUG_MODE) {
            //fire and forget writing log file
            doAsync {
                var savedOK = true
                try {
                    when (result) {
                        is Result.Success -> {
                            writeTextFile(File(result.fileDir.absolutePath +
                                    File.separatorChar +
                                    result.filename +
                                    ".DEBUG_MODE.dump"
                            ), result.debugMessage)
                        }
                        is Result.Fail -> {
                            if (result.fileDir != null && result.fileDir.exists() && result.filename != null) {
                                val rootPath = result.fileDir.absolutePath + File.separatorChar + result.filename
                                writeTextFile(File("$rootPath.DEBUG_MODE.dump"), result.debugMessage)
                                writeTextFile(File("$rootPath.error.dump"), result.errorMessage)
                            } else {
                                savedOK = false
                                Log.e(LOG_TAG_MAIN, result.errorMessage.toString())
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    savedOK = false
                }
                uiThread {
                    if (savedOK) toast(getString("logs_written"), Toast.LENGTH_SHORT)
                    else toast(getString("logs_write_error"), Toast.LENGTH_SHORT)
                }
            }
        }

        when (result) {
            is Result.Success -> {

                val exportedFilePath = "${result.fileDir}${File.separator}${result.filename}"
                writeToCircularBuffer(exportedFilePath, sharedPreferences)

                val circularBuffer = readCircularBuffer(sharedPreferences)
                if(DEBUG_MODE) circularBuffer.forEach { Log.i("CIRC_BUFFER", it) }

                val resultsParcel = ViewResultsParcel(
                        getString("stats_label"),
                        result.publicMessage,
                        result.fileDir.absolutePath,
                        circularBuffer
                )

                val intent = Intent(this, ViewResultsActivity::class.java).apply {
                    putExtra(EXTRA_MESSAGE_VIEW_RESULTS, resultsParcel)
                }
                startActivity(intent)
            }
            is Result.Fail -> {
                fail.failGracefully(this, result.errorMessage.toString())
            }
        }
    }

    private fun disableExecutive() {
        // disable executive UI
        btnExport.isEnabled = false
        progressBar.visibility = ProgressBar.VISIBLE
    }

    private fun enableExecutive() {
        // enable executive UI
        btnExport.isEnabled = true
        progressBar.visibility = ProgressBar.INVISIBLE
    }

    //  CALLBACKS END

    fun directoryPick(@Suppress("UNUSED_PARAMETER") v: View) {
        if (!permWriteIsGranted()) {
            toast(getString("permission_needed"), Toast.LENGTH_LONG)
            return
        }
        val message: String = getString("pick_dir_message")
        try {
            ActionTools.actionPickDir(this, REQUEST_CODE_OPEN_DIRECTORY, message)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            fail.failGracefully(this, e.localizedMessage)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY &&
                resultCode == Activity.RESULT_OK) {
            try {
                val fileUri = data!!.data
                val exportDir: String? = fileUri!!.path
                if (!exportDir.isNullOrEmpty()) {
                    handleDirectoryChoice(exportDir)
                } else {
                    Log.w(LOG_TAG_MAIN, " exportDir = null or empty")
                }
            } catch (e: Exception) {
                fail.failGracefully(this, " Error 7")
                return
            }
        } else {
            Log.w(LOG_TAG_MAIN, "Nothing selected")
        }
    }

    private fun handleDirectoryChoice(selectedDir: String) {
        Log.i(LOG_TAG_MAIN, "SELECTED_DIR: $selectedDir")
        val newRoot: File? = File(selectedDir)
        if (storageDirExists(newRoot)) {
            setRoot(newRoot, exportListener, sharedPreferences, getString("last_seen_root"))
            setTvRootDir()
            Log.i(LOG_TAG_MAIN, "new root: ${getRoot(exportListener)}")
        } else {
            setTvRootDir()
            Log.e(LOG_TAG_MAIN, "storageDirExists(newRoot) == false - should never happen")
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
        // if root is stored and exists set it
        if (sharedPreferences.contains(this.getString("last_seen_root"))) {
            val lastSeenRoot = File(sharedPreferences.getString(this.getString("last_seen_root"),
                    locusInfo().rootDirExport))
            if (lastSeenRoot.isDirectory) {
                setRoot(lastSeenRoot, exportListener, sharedPreferences, getString("last_seen_root"))
            } else Log.e(LOG_TAG_MAIN, "lastSeenRootIsNotDir!")
            return
        }
        val locusExportDir = File(locusInfo().rootDirExport)
        if (storageDirExists(locusExportDir)) {
            setRoot(locusExportDir, exportListener, sharedPreferences, getString("last_seen_root"))
        } else {
            val appExportRoot = File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS), this.getString("app_name"))
            val created: Boolean = appExportRoot.mkdirs()
            if (created) {
                Log.i(LOG_TAG_MAIN, "setAppStorageRoot() - directory created")
            } else {
                Log.i(LOG_TAG_MAIN, "setAppStorageRoot() - directory not created")
            }
            setRoot(appExportRoot, exportListener, sharedPreferences, getString("last_seen_root"))
        }
    }

    // PERMISSION UTILS

    override fun permWriteIsGranted(): Boolean {
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

    override fun toast(key: String, length: Int) {
        val toast = Toast.makeText(applicationContext, key, length)
        toast.show()
    }
}

