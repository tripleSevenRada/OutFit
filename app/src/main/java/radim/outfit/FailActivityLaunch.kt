package radim.outfit

import android.content.Intent
import android.support.v4.content.ContextCompat.startActivity

import android.support.v7.app.AppCompatActivity
import android.util.Log

class FailGracefullyLauncher{
    fun failGracefully(ctx: AppCompatActivity, message: String){
        Log.w("FAIL_GRACEFULLY", "FAILING WITH $ctx WITH MESSAGE: $message")
        val intent = getFinnishIntent(ctx, message)
        startActivity(ctx, intent, null)
    }

    private fun getFinnishIntent(ctx: AppCompatActivity, message: String): Intent {
        return Intent(ctx, FinishGracefully::class.java).apply {
            putExtra(EXTRA_MESSAGE_FINISH, message)
        }
    }
}