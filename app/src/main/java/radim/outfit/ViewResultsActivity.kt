package radim.outfit

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.activity_view_results.*
import kotlinx.android.synthetic.main.content_stats.*
import radim.outfit.core.connectiq.getListOfFitFiles
import java.io.File
import java.lang.StringBuilder

class ViewResultsActivity : AppCompatActivity() {

    private val tag = "VIEW_RESULTS"
    private lateinit var parcel: ViewResultsParcel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(tag,"onCreate")
        setContentView(R.layout.activity_view_results)
        if(intent.hasExtra(EXTRA_MESSAGE_VIEW_RESULTS))
            parcel = intent.getParcelableExtra(EXTRA_MESSAGE_VIEW_RESULTS)

        tvViewResultsLabel.text = getString("stats_label")

        val messagesAsStringBuilder = StringBuilder()

        if(::parcel.isInitialized) {
            parcel.messages.forEach { with(messagesAsStringBuilder) { append(it); append("\n") } }
            tvViewResults.text = messagesAsStringBuilder.toString()
            Log.i(tag, "path to parent dir: " + parcel.parentDir)
            Log.i(tag, "path to .fit: " + parcel.fitFileAbsPath)
        }
        progressBarView.visibility = ProgressBar.INVISIBLE
    }

    override fun onStart() {
        super.onStart()
        Log.i(tag,"onStart")

        if (::parcel.isInitialized) {
            val fitFiles = getListOfFitFiles(File(parcel.parentDir))
            fitFiles.forEach { Log.i(tag, ".fit file to serve: ${it.absolutePath}") }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(tag,"onResume")

    }

    override fun onPause() {
        super.onPause()
        Log.i(tag,"onPause")

    }

    override fun onStop() {
        super.onStop()
        Log.i(tag,"onStop")

    }

}
