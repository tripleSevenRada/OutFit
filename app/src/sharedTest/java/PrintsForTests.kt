package radim.outfit

import android.util.Log

fun <T>printArray(array: Array<T>){
    Log.i("CB_TEST","_________________________________________________________")
    array.forEach { Log.i("GEN_ARR_PRINT","${it.toString()}, ") }
}