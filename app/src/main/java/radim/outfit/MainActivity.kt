package radim.outfit

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.content.Intent
import android.util.Log
import java.io.File

const val REQUEST_CODE_OPEN_DIRECTORY = 9999

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    @Suppress("unused")
    fun directoryPick(@Suppress("UNUSED_PARAMETER")v: View){
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        startActivityForResult(Intent.createChooser(intent, "Choose directory"), REQUEST_CODE_OPEN_DIRECTORY)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY && resultCode == Activity.RESULT_OK) {
            try {
                Log.i("TEST", "Result URI " + data!!.data!!)
                val pathS = data.data!!.path!!
                Log.i("TEST", "Result PATH $pathS")
                val file = File(pathS)
                Log.i("TEST", "file.absolutePath ${file.absolutePath}")
                Log.i("TEST", "file.isDirectory ${file.isDirectory}")
                Log.i("TEST", "file.toString $file")
            }catch (e: Exception){
                e.printStackTrace()
                // TODO
            }

        }
    }

}
