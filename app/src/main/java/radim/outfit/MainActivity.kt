package radim.outfit

import android.Manifest
import android.app.Activity
import android.support.v7.app.AppCompatActivity
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
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import radim.outfit.core.Filename
import radim.outfit.core.Stats

const val LOG_TAG = "MAIN"
const val REQUEST_CODE_OPEN_DIRECTORY = 9999
const val REQUEST_CODE_PERM_WRITE_EXTERNAL = 7777

fun AppCompatActivity.getString(name: String): String {
    return resources.getString(resources.getIdentifier(name, "string", packageName))
}

class MainActivity : AppCompatActivity() {

    private var appExportRoot: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (isExternalStorageWritable()) {
            Log.i(LOG_TAG, "isExternalStorageWritable() == true")
        } else {
            Log.e(LOG_TAG, "isExternalStorageWritable() == false")
            // TODO finish gracefully
        }

        if (permWriteIsGranted()) {
            setAppStorageRoot()
            setTvRootDir()
        } else {
            requestPermWrite()
        }

        if (LocusUtils.isIntentTrackTools(this.intent)) {
            // event performed if user tap on your app icon in tools menu of 'Track'
            handleIntentTrackToolsMenu(this, this.intent)
        }
    }

    // isIntentTrackTools(intent) = true
    private fun handleIntentTrackToolsMenu(act: Activity, intent: Intent) {
        var track: Track? = null
        try {
            track = LocusUtils.handleIntentTrackTools(act, intent)
        } catch (e: RequiredVersionMissingException) {
            // TODO finish gracefully
        } catch (e: Exception) {
            // TODO finish gracefully
        }
        if (track != null && track.points != null && track.points.size > 0) {
            // do work
            tvStats.text = Stats().basicInfo(track, this)
            etFilename.setText(Filename().getFilename(track))
        } else {
            // TODO finish gracefully
        }
    }

    fun export(@Suppress("UNUSED_PARAMETER") v: View){
        //TODO
    }

    fun directoryPick(@Suppress("UNUSED_PARAMETER") v: View) {
        if(!permWriteIsGranted()) toast(getString("permission_needed"), Toast.LENGTH_SHORT)
        val rootI = appExportRoot
        val rootPath = rootI?.path ?: ""
        val chooserIntent = Intent(this, DirectoryChooserActivity::class.java)
        Log.i(LOG_TAG, "calling DirectoryChooserActivity with root: $rootI")

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
                    // TODO
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
            appExportRoot = newRoot
            setTvRootDir()
            Log.i(LOG_TAG, "new root: $appExportRoot")
        } else {
            appExportRoot = null
            setTvRootDir()
            Log.e(LOG_TAG, "storageDirExists(newRoot) == false - should never happen")
        }
    }

    private fun setTvRootDir() {
        val text: String? = appExportRoot?.path
        if (text != null){
            tvRootDir.setTextColor(this.getColor(R.color.imitateButtons))
            tvRootDir.text = text
        } else {
            tvRootDir.setTextColor(this.getColor(R.color.colorAccent))
            tvRootDir.text = this.getString("not_set")
        }
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
        appExportRoot = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), this.getString("app_name"))
        val created: Boolean = appExportRoot?.mkdirs() ?: false
        if (created) {
            Log.i(LOG_TAG, "setAppStorageRoot() - directory created")
        } else {
            Log.i(LOG_TAG, "setAppStorageRoot() - directory not created")
        }
        if(!storageDirExists(appExportRoot)){
            appExportRoot = null
            Log.e(LOG_TAG,"storageDirExists(appExportRoot) = false: this should never happen here")
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
                    appExportRoot = null
                    setTvRootDir()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun toast(message: String, duration: Int){
        val toast = Toast.makeText(applicationContext, message, duration)
        toast.show()
    }
}