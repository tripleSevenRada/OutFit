package radim.outfit

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_finish_gracefully.*

class FinishGracefully : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finish_gracefully)
        val message = if (intent.hasExtra(EXTRA_MESSAGE_FINISH)){
            intent.getStringExtra(EXTRA_MESSAGE_FINISH)
        } else {
            ""
        }
        activity_finish_gracefullyTVData.text = message
    }
}
