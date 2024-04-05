package co.present.present.service

import android.graphics.Bitmap
import android.net.Uri
import com.facebook.FacebookSdk
import java.io.File
import java.io.FileOutputStream

/**
 * Wrapper object for the Android file system so we can test classes interacting with it without
 * Robolectric.
 */
class Filesystem {

    fun fromFilesDir(filename: String): File {
        return File(FacebookSdk.getApplicationContext().filesDir, filename)
    }

    fun uriFromFile(file: File): Uri {
        return Uri.fromFile(file)
    }

    fun writeToFile(bitmap: Bitmap, file: File) {
        with(FileOutputStream(file)) {
            // This doesn't actually compress anything because PNG is a lossless format
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, this)
            close()
        }
    }
}