package co.present.present.extensions

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import co.present.present.R
import co.present.present.location.CoverPhotoUrlLoader
import co.present.present.model.Circle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader
import jp.wasabeef.glide.transformations.CropCircleTransformation

fun ImageView.loadCircularImage(url: String?,
                                @DrawableRes placeholder: Int = R.drawable.circle_light_gray,
                                skipMemoryCache: Boolean = false,
                                diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.SOURCE) {

    load(url = url,
            placeholder = placeholder,
            bitmapTransform = CropCircleTransformation(context),
            skipMemoryCache = skipMemoryCache,
            diskCacheStrategy = diskCacheStrategy)
}

fun ImageView.loadCircularImageFromUri(uri: Uri, @DrawableRes placeholder: Int = R.drawable.circle_light_gray,
                                       skipMemoryCache: Boolean = false,
                                       diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.SOURCE) {
    loadFromUri(uri = uri,
            placeholder = placeholder,
            bitmapTransform = CropCircleTransformation(context),
            skipMemoryCache = skipMemoryCache,
            diskCacheStrategy = diskCacheStrategy)
}

fun ImageView.load(url: String?,
                   @DrawableRes placeholder: Int = R.color.lightGray,
                   bitmapTransform: Transformation<Bitmap>? = null,
                   skipMemoryCache: Boolean = false,
                   diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.SOURCE) {
    Glide.with(context)
            .using(DefaultUrlLoader(context))
            .load(url)
            .skipMemoryCache(skipMemoryCache)
            .diskCacheStrategy(diskCacheStrategy)
            .placeholder(placeholder).apply {
        bitmapTransform?.let { bitmapTransform(bitmapTransform) }
    }
            .into(this)
}

fun ImageView.loadFromUri(uri: Uri,
                          @DrawableRes placeholder: Int = R.color.lightGray,
                          bitmapTransform: Transformation<Bitmap>? = null,
                          skipMemoryCache: Boolean = false,
                          diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.SOURCE) {
    Glide.with(context)
            .load(uri)
            .skipMemoryCache(skipMemoryCache)
            .diskCacheStrategy(diskCacheStrategy)
            .placeholder(placeholder).apply {
        bitmapTransform?.let { bitmapTransform(bitmapTransform) }
    }
            .into(this)
}

fun ImageView.loadCircleCoverImage(circle: Circle) {
    loadCoverImage(circle, CropCircleTransformation(context), R.drawable.circle_light_gray)
}

fun ImageView.loadCoverImage(circle: Circle, bitmapTransform: Transformation<Bitmap>? = null, placeholder: Int = R.color.lightGray) {
    Glide.with(context)
            .using(CoverPhotoUrlLoader(context))
            .load(circle)
            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
            .placeholder(placeholder)
            .apply {
        bitmapTransform?.let { bitmapTransform(bitmapTransform) }
    }
            .into(this)
}

class DefaultUrlLoader(context: Context) : BaseGlideUrlLoader<String>(context) {
    override fun getUrl(original: String, width: Int, height: Int): String {
        // Construct the url for the correct size here.
        return "$original=w$width-h$height-n-rj"
    }
}