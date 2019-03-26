package radim.outfit

import android.Manifest
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity

fun permWriteIsGranted(ctx: AppCompatActivity): Boolean {
    return ContextCompat.checkSelfPermission(ctx,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
}

fun requestPermWrite(ctx: AppCompatActivity) {
    ActivityCompat.requestPermissions(ctx,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_PERM_WRITE_EXTERNAL)
}