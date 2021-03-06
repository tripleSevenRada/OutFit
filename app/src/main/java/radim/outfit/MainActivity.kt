package radim.outfit

import android.annotation.TargetApi
import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.os.Bundle
import android.view.View
import android.os.Environment
import android.util.Log
import locus.api.android.utils.LocusUtils
import locus.api.android.utils.exceptions.RequiredVersionMissingException
import java.io.File
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_export.*
import kotlinx.android.synthetic.main.content_path.*
import locus.api.android.utils.LocusInfo
import radim.outfit.core.export.logic.*
import locus.api.android.ActionTools
import radim.outfit.core.export.work.*
import radim.outfit.core.export.work.locusapiextensions.track_preprocessing.TrackContainer
import radim.outfit.core.share.work.*
import radim.outfit.core.timer.SimpleTimer
import radim.outfit.core.timer.Timer
import radim.outfit.core.viewmodels.MainActivityViewModel
import java.lang.RuntimeException

const val LOG_TAG_MAIN = "MAIN"
const val REQUEST_CODE_OPEN_DIRECTORY = 9999
const val REQUEST_CODE_PERM_WRITE_EXTERNAL = 7777
const val EXTRA_MESSAGE_FINISH = "start finish gracefully activity to explain what happened"
const val EXTRA_MESSAGE_VIEW_RESULTS = "start view results activity with ViewResultParcel extra"

fun AppCompatActivity.getString(name: String): String {
    return try {
        resources.getString(resources.getIdentifier(name, "string", packageName))
    } catch (e: Exception) {
        "*"
    }
}

const val DEBUG_MODE = false

class MainActivity : AppCompatActivity(),
        TriggerActionProvider,
        LastSelectedValuesProvider,
        PermInfoProvider,
        TrackDetailsProvider,
        ExportOptionsDataProvider,
        Toaster {

    private val tvStatsFiller = " \n \n \n "
    private val debugMessages = mutableListOf<String>()

    private lateinit var sharedPreferences: SharedPreferences
    private val fail = FailGracefullyLauncher()

    private fun showSpeedPickerDialog() {
        val viewModel =
                ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
        if (viewModel.speedPickerFragmentShown) return
        try {
            val fm = supportFragmentManager
            val spf = SpeedPickerFragment()
            spf.show(fm, "speed_picker_fragment")
            viewModel.speedPickerFragmentShown = true
        }catch (e: Exception){
            Log.e(LOG_TAG_MAIN, e.localizedMessage)
        }
    }

    // Menu START
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.explain_waypoints -> {
                startActivity(Intent(this, ExplainWaypointsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    // Menu END

    // SpeedPickerFragment interfaces impl START
    override fun getTriggerAction(): (Float) -> Unit = exportListener.getOkAction()

    override fun getSpeedMperS(): Float {
       return sharedPreferences.getFloat(getString("last_seen_speed_value_m_s"), SPEED_DEFAULT_M_S)
    }

    override fun setSpeedMperS(value: Float) {
        persistInSharedPreferences(getString("last_seen_speed_value_m_s"), value)
    }

    override fun onDialogDismissed() {
        val viewModel =
                ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
        viewModel.speedPickerFragmentShown = false
    }

    override fun getUnitsButtonS():String = sharedPreferences.getString(getString("last_seen_speed_units_string"), DEFAULT_UNITS_RADIO_BUTTON)?: "error"
    override fun setUnitsButtonS(idS: String) = persistInSharedPreferences(getString("last_seen_speed_units_string"), idS)

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
                is String -> {
                    putString(key, value)
                }
                else -> {
                    throw RuntimeException("unimplemented")
                }
            }
            apply()
        }
    }

    override fun getLengthInM(): Int {
        val viewModel =
                ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
        val length: Double? = viewModel.trackTotalLength
        return if (length == null) {
            // sanity check, should never happen
            FailGracefullyLauncher().failGracefully(this, "Null trackContainer length.")
            finish()
            200
        } else length.toInt()
    }

    override fun getActivityType(): Int {
        val viewModel =
                ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
        return viewModel.activityType ?: 100
    }
    // SpeedPickerFragment interfaces impl END

    // ExportOptionsDataProvider impl START
    override fun getBundle(): Boolean = sharedPreferences.getBoolean("bundle_nav_hints", false)
    override fun setBundle(value: Boolean) = sharedPreferences.edit().putBoolean("bundle_nav_hints", value).apply()
    override fun getMove(): Boolean = sharedPreferences.getBoolean("move_nav_hints", false)
    override fun setMove(value: Boolean) = sharedPreferences.edit().putBoolean("move_nav_hints", value).apply()
    override fun getMoveDist(): String = sharedPreferences.getString("move_nav_hints_dist", "10")?:"10"
    override fun setMoveDist(value: String) = sharedPreferences.edit().putString("move_nav_hints_dist", value).apply()
    override fun getBundleDist(): String = sharedPreferences.getString("bundle_nav_hints_dist", "90")?:"90"
    override fun setBundleDist(value: String) = sharedPreferences.edit().putString("bundle_nav_hints_dist", value).apply()
    // ExportOptionsDataProvider impl END

    inner class TimerCallback(private val viewModel: MainActivityViewModel) : Timer.TimerCallback {
        override fun tick(): Boolean {
            return if (!viewModel.exportInProgress) {
                enableExecutive(viewModel)
                false
            } else true
        }
    }

    private lateinit var simpleTimer: SimpleTimer
    // https://drive.google.com/file/d/1wwYzoPQts1HreDpS614oMAVyafU07ZYF/view?usp=sharing
    private lateinit var exportListener: ExportListener

    //override fun onSaveInstanceState(outState: Bundle?) {
    //super.onSaveInstanceState(outState)
    //}

    private var circularBufferPaths = CircularBufferWithSharedPrefs(1) // id 1
    private var circularBufferCourseNames = CircularBufferWithSharedPrefs(2) // id 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.title = getString("activity_main_label")

        content_exportTVStatsData?.text = tvStatsFiller // do not flick or "inflate" visibly

        val viewModel =
                ViewModelProviders.of(this).get(MainActivityViewModel::class.java)

        // Create the observer of trackContainer
        val trackContainerObserver = Observer<TrackContainer> { changed ->
            // Action
            changed?.let { onTrackContainerChanged(changed, viewModel) }
        }

        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        viewModel.trackContainer.observe(this, trackContainerObserver)

        simpleTimer = SimpleTimer(400, TimerCallback(viewModel))
        debugMessages.add("Debug:")
        exportListener = ExportListener(
                ExportFunction(),
                ExportPOJO(null, null, null),
                ::exportListenerCallback,
                ::disableExecutive,
                ::showSpeedPickerDialog,
                this,
                this,
                this,
                viewModel,
                debugMessages
        )

        Log.i(LOG_TAG_MAIN, "export in progress: ${viewModel.exportInProgress}")
        if (viewModel.exportInProgress) {
            disableExecutive()
            simpleTimer.start()
        } else enableExecutive(viewModel)

        sharedPreferences = this.getSharedPreferences(
                getString(R.string.main_activity_preferences), Context.MODE_PRIVATE)
        initSharedPrefs(this)
        with(sharedPreferences.edit()) {
            if (!sharedPreferences.contains("${CIRC_BUFF_POINTER_KEY_PREFIX}1"))
                circularBufferPaths.initCircularBuffer(this)
            if (!sharedPreferences.contains("${CIRC_BUFF_POINTER_KEY_PREFIX}2"))
                circularBufferCourseNames.initCircularBuffer(this)
            apply()
        }

        //showSpeedPickerDialog()  // if you want to test speed picker layout on emulator only!

        val activeLocus = LocusUtils.getActiveVersion(this)
        if (activeLocus == null) {
            fail.failGracefully(this, this.getString("locus_not_installed") + " Error 1")
            finish()
            return
        }

        try {
            ActionTools.getLocusInfo(this, activeLocus)
        } catch (e: RequiredVersionMissingException) {
            fail.failGracefully(this, this.getString("required_version_missing - ") + e.localizedMessage + " Error 2")
            finish()
            return
        }

        if (!isExternalStorageWritable()) {
            fail.failGracefully(this, "isExternalStorageWritable() == false" + " Error 3")
            finish()
            return
        }

        content_pathETFilename?.filters = arrayOf(FilenameCharsFilter())
        content_exportBTNExport?.setOnClickListener(exportListener)
        if (content_pathETFilename != null) exportListener.attachView(content_pathETFilename)
        exportListener.attachDefaultFilename(this.getString("default_filename"))

        if (permWriteIsGranted()) {
            setAppStorageRoot()
            setTvRootDir()
        } else {
            requestPermWrite(this)
        }

        Log.i(LOG_TAG_MAIN, "activeLocus.versionName: ${activeLocus.versionName}")
        Log.i(LOG_TAG_MAIN, "activeLocus.versionCode: ${activeLocus.versionCode}")
        val info = locusInfo()
        if (!info.isRunning) toast(getString("locus_not_running"), Toast.LENGTH_LONG)

        if (LocusUtils.isIntentTrackTools(this.intent)) {
            // event performed if user tap on your app icon in tools menu of 'Track'
            handleIntentTrackToolsMenu(this.intent, viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE)
        if (btManager is BluetoothManager) {
            val btAdapter = btManager.adapter
            val state = btAdapter?.state
            if (state == BluetoothAdapter.STATE_OFF) {
                Log.w("onResumeMain", "onResume BT STATE OFF")
                val checkboxInShare = sharedPreferences.getBoolean((getString("checkbox_cciq")), true)
                val viewModel =
                        ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
                if (checkboxInShare && viewModel.btWarningShownCounter < 3) {
                    Toast.makeText(this, getString("bt_may_be_needed"), Toast.LENGTH_LONG).show()
                    viewModel.btWarningShownCounter ++
                }
            }
        }
    }

    // isIntentTrackTools(intent) = true
    private fun handleIntentTrackToolsMenu(intent: Intent,
                                           viewModel: MainActivityViewModel) {
        Log.i(LOG_TAG_MAIN, "handleIntentTrackToolsMenu main activity: $this")
        if (viewModel.trackContainerFinished) return
        if (viewModel.preprocessInProgress) {
            this.disableExecutive(); return
        }
        this.disableExecutive()
        viewModel.buildTrackContainer(intent, debugMessages)
    }

    private fun onTrackContainerChanged(newTrackContainer: TrackContainer, viewModel: MainActivityViewModel) {
        if (newTrackContainer.failedMessage.isEmpty()) {
            trackInit(newTrackContainer)
        } else {
            fail.failGracefully(this, newTrackContainer.failedMessage)
            finish()
        }
        this.enableExecutive(viewModel)
    }

    private fun trackInit(trackContainer: TrackContainer) {
        content_exportTVStatsData?.text = Stats().basicInfo(trackContainer.track, this)
        val filename = getFilename(trackContainer.track.name, getString("default_filename"))
        content_pathETFilename?.setText(filename)
        setTrack(trackContainer, exportListener)
        setFilename(filename, exportListener)
    }

    //  CALLBACKS START

    private fun exportListenerCallback(result: Result, viewModel: MainActivityViewModel) {

        enableExecutive(viewModel)

        if (DEBUG_MODE) {
            //fire and forget writing log file
            writeLog(result, this)
        }

        when (result) {
            is Result.Success -> {
                val lab = FilenameCoursenamePair()
                val exportedFilePath = "${result.fileDir}${File.separator}${result.filename}"
                circularBufferPaths.writeToCircularBuffer(exportedFilePath, sharedPreferences)
                circularBufferCourseNames.writeToCircularBuffer(lab.getEntry(
                        Pair(
                                result.filename,
                                result.coursename
                        )), sharedPreferences)
                val cbPathsArray = circularBufferPaths.readCircularBuffer(sharedPreferences)
                if (DEBUG_MODE) cbPathsArray.forEach { Log.i("CIRC_BUFF_PATHS", it) }
                val cbPathsArrayReduced = cbPathsArray.ridEmpty().ridDuplicities()
                val cbFilenamesToCoursenamesArray =
                        circularBufferCourseNames.readCircularBuffer(sharedPreferences)
                if (DEBUG_MODE) cbFilenamesToCoursenamesArray.forEach { Log.i("CIRC_BUFF_F_TO_C", it) }
                val cbFilenamesToCoursenamesArrayReversed =
                        cbFilenamesToCoursenamesArray.reversedArray()
                if (DEBUG_MODE) cbFilenamesToCoursenamesArrayReversed.forEach { Log.i("CIRC_BUFF_F_TO_C_R", it) }
                val filenamesToCourseNames = mutableMapOf<String, String>()
                cbFilenamesToCoursenamesArrayReversed.forEach {
                    if (it.isNotEmpty()) {
                        val pair = lab.getPair(it)
                        filenamesToCourseNames[pair.first] = pair.second
                    }
                }

                val resultsParcel = ViewResultsParcel(
                        getString("stats_label"),
                        result.publicMessage,
                        cbPathsArrayReduced,
                        filenamesToCourseNames
                )

                val intent = Intent(this, ViewResultsActivity::class.java).apply {
                    putExtra(EXTRA_MESSAGE_VIEW_RESULTS, resultsParcel)
                }
                startActivity(intent)
                finish()
            }
            is Result.Fail -> {
                fail.failGracefully(this, result.errorMessage.toString())
                finish()
            }
        }
    }

    private fun disableExecutive() {
        if (DEBUG_MODE) Log.i(LOG_TAG_MAIN, "DISABLE_Executive; Activity: $this")
        // disable executive UI
        content_exportBTNExport?.isEnabled = false
        content_pathBTNExportOptions?.isEnabled = false
        activity_mainPB?.visibility = ProgressBar.VISIBLE
    }

    private fun enableExecutive(viewModel: MainActivityViewModel) {
        if (DEBUG_MODE) Log.i(LOG_TAG_MAIN, "ENABLE_Executive; Activity: $this")
        // enable executive UI if export is not running and preprocess is not running
        if (!viewModel.exportInProgress && !viewModel.preprocessInProgress) {
            content_exportBTNExport?.isEnabled = true
            content_pathBTNExportOptions?.isEnabled = true
            activity_mainPB?.visibility = ProgressBar.INVISIBLE
        }
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
            finish()
        }
    }

    fun exportOptions(@Suppress("UNUSED_PARAMETER") v: View) {
        val viewModel =
                ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
        if(viewModel.exportOptionsDisplayed) return
        viewModel.exportOptionsDisplayed = true
        try {
            val fm = supportFragmentManager
            val eof = ExportOptionsFragment()
            eof.show(fm, "export_options_fragment")
        }catch (e: Exception) {
            Log.e(LOG_TAG_MAIN, e.localizedMessage)
        }
    }

    override fun onExportOptionsDismiss() {
        val viewModel =
                ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
        viewModel.exportOptionsDisplayed = false
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
                finish()
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

    @TargetApi(23)
    private fun setTvRootDir() {
        val text: String? = getRoot(exportListener)?.path
        val apiLevel = android.os.Build.VERSION.SDK_INT
        if (text != null) {
            if (apiLevel >= 23) content_pathTVRootDirPath?.setTextColor(this.getColor(R.color.imitateButtons))
            content_pathTVRootDirPath?.text = text
        } else {
            if (apiLevel >= 23) content_pathTVRootDirPath?.setTextColor(this.getColor(R.color.colorAccent))
            content_pathTVRootDirPath?.text = this.getString("not_set")
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

    private fun storageDirExists(file: File?): Boolean =
            try {
                (file != null && file.isDirectory)
            } catch (e: Exception) {
                false
            }

    private fun setAppStorageRoot() {
        // if root is stored and exists set it
        if (sharedPreferences.contains(this.getString("last_seen_root"))) {
            val lastSeenRoot = File(sharedPreferences.getString(this.getString("last_seen_root"),
                    locusInfo().rootDirExport))
            if (try {
                        lastSeenRoot.isDirectory
                    } catch (e: Exception) {
                        false
                    }) {
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
        return permWriteIsGranted(this)
        //return ContextCompat.checkSelfPermission(this,
          //      Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

   /* private fun requestPermWrite() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_PERM_WRITE_EXTERNAL)
    }*/

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

