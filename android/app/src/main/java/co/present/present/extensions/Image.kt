package co.present.present.extensions

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import android.widget.ImageView
import okio.ByteString
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream

@Throws(FileNotFoundException::class)
fun Uri.toImageBitmap(contentResolver: ContentResolver): Bitmap? {
    return toInputStream(contentResolver)?.toBitmap()
}

@Throws(FileNotFoundException::class)
private fun Uri.toInputStream(contentResolver: ContentResolver): InputStream? {
    return contentResolver.openInputStream(this)
}

fun Bitmap.toByteString(): ByteString {
    with(ByteArrayOutputStream()) {
        compress(Bitmap.CompressFormat.JPEG, 100, this)
        close()
        return ByteString.of(*this.toByteArray())
    }
}

private fun InputStream.toBitmap(): Bitmap? {
    return BitmapFactory.decodeStream(this)
}

fun ImageView.setImageResourceWithTint(@DrawableRes imageResId: Int, @ColorRes colorResId: Int) {
    var drawable: Drawable = VectorDrawableCompat.create(resources, imageResId, null)!!
    drawable = DrawableCompat.wrap(drawable)
    DrawableCompat.setTint(drawable, ContextCompat.getColor(context, colorResId))
    setImageDrawable(drawable)
}