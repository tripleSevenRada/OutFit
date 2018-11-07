package radim.outfit

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.content.Intent
import android.util.Log
import net.rdrei.android.dirchooser.DirectoryChooserActivity
import net.rdrei.android.dirchooser.DirectoryChooserConfig


const val LOG_TAG = "MAIN"
const val REQUEST_CODE_OPEN_DIRECTORY = 9999

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun directoryPick(@Suppress("UNUSED_PARAMETER")v: View){
        val chooserIntent = Intent(this, DirectoryChooserActivity::class.java)

        val config = DirectoryChooserConfig.builder()
                .newDirectoryName("${getString(R.string.app_name)}${getString(R.string.new_directory_name)}")
                .allowReadOnlyDirectory(false)
                .allowNewDirectoryNameModification(true)
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
                }catch(e: Exception){
                    e.printStackTrace()
                    //TODO
                }
            } else {
                // Nothing selected
                //TODO
            }
        }
    }

    private fun handleDirectoryChoice(selectedDir: String){
        Log.i(LOG_TAG,"SELECTED_DIR: $selectedDir")
    }
}
