package radim.outfit

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import kotlinx.android.synthetic.main.activity_explain_waypoints.*

class ExplainWaypointsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_explain_waypoints)

        // in app.iml
        // option name="ASSETS_FOLDER_RELATIVE_PATH" value="/src/main/assets"
        supportActionBar?.title = getString("activity_explain_waypoints_label")

        val myWebView: WebView = activity_explain_waypoints_WV
        myWebView.loadUrl("file:///android_asset/explainAboutWaypoints/wpts_mapped_to_coursepoints.html")

    }
}
