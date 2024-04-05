package co.present.present.service

import android.app.Application
import android.graphics.Bitmap
import androidx.annotation.DimenRes
import co.present.present.R
import com.bumptech.glide.Glide

/**
 * A wrapper object for network requests to download bitmaps using Glide,
 * so we can test without Robolectric.
 */
class BitmapDownloader {

    fun download(application: Application, url: String, @DimenRes diameterRes: Int): Bitmap {
        val diameter = application.resources.getDimensionPixelSize(R.dimen.avatar_large_dimen)
        return Glide.with(application)
                .load(url)
                .asBitmap()
                .into(diameter, diameter)
                .get()
    }
}