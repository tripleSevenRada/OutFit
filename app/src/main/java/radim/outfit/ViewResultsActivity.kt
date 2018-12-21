package radim.outfit

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.activity_view_results.*
import kotlinx.android.synthetic.main.content_stats.*
import java.lang.StringBuilder

class ViewResultsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_results)
        val parcel: ViewResultsParcel? = if(intent.hasExtra(EXTRA_MESSAGE_VIEW_RESULTS))
            intent.getParcelableExtra<ViewResultsParcel>(EXTRA_MESSAGE_VIEW_RESULTS)
        else null

        tvViewResultsLabel.text = getString("stats_label")
        val messagesAsStringBuilder = StringBuilder()
        parcel?.messages?.forEach { with(messagesAsStringBuilder){append(it); append("\n")} }
        tvViewResults.text = messagesAsStringBuilder.toString()
        Log.i("ViewResults","path to .fit: " + parcel?.fitFileAbsPath)

        progressBarView.visibility = ProgressBar.INVISIBLE

    }

}
