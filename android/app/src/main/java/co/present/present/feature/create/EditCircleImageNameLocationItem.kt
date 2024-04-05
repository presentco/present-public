package co.present.present.feature.create

import android.net.Uri
import android.widget.TextView
import co.present.present.R
import co.present.present.extensions.loadCircularImage
import co.present.present.extensions.loadCircularImageFromUri
import co.present.present.extensions.updateCompoundDrawablesRelative
import co.present.present.feature.common.Payload
import co.present.present.feature.common.item.EditableTextItem
import co.present.present.feature.common.item.OnTextChangedListener
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.location.places.Place
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_edit_circle_name_location_photo.*
import kotlin.properties.Delegates

data class EditCircleImageNameLocationItem(private val coverImageUrl: String? = null,
                                           val name: String,
                                           val onNameChangedListener: OnTextChangedListener,
                                           val editImageListener: EditImageListener,
                                           val place: Place?,
                                           val editLocationListener: EditLocationListener)
    : EditableTextItem(R.string.name_your_circle, onNameChangedListener, name) {

    var coverImageUri: Uri by Delegates.observable(Uri.EMPTY) { _, _, _ ->
        notifyChanged(PayloadPhoto)
    }

    override fun getLayout() = R.layout.item_edit_circle_name_location_photo

    override fun bind(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PayloadLocation)) {
            bindLocation(holder)
            return
        }
        if (payloads.contains(PayloadPhoto)) {
            bindPhoto(holder)
            return
        }
        if (payloads.contains(Payload.DontUpdate)) return

        super.bind(holder, position, payloads)
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        super.bind(viewHolder, position)
        bindPhoto(viewHolder)
        bindLocation(viewHolder)
    }

    private fun bindPhoto(viewHolder: ViewHolder) {
        if (coverImageUri == Uri.EMPTY) {
            viewHolder.photo.loadCircularImage(coverImageUrl, placeholder = R.drawable.circle_purple)
        } else {
            // Don't allow Glide to cache the image, otherwise it won't load a new one from
            // the same URI if the user tries a end time
            viewHolder.photo.loadCircularImageFromUri(coverImageUri, skipMemoryCache = true, diskCacheStrategy = DiskCacheStrategy.NONE)
        }
        viewHolder.photo.setOnClickListener { editImageListener.onEditImageClicked(this) }
    }

    private fun bindLocation(viewHolder: ViewHolder) {
        viewHolder.location.updateCompoundDrawablesRelative(end = R.drawable.ic_carat_gray)
        viewHolder.location.setText(place?.getUserVisibleName() ?: "", TextView.BufferType.NORMAL)
        viewHolder.location.setOnClickListener { editLocationListener.onEditLocationClicked(this) }
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is EditCircleImageNameLocationItem
    }

    override fun getChangePayload(newItem: com.xwray.groupie.Item<*>?): Any? {
        if (newItem !is EditCircleImageNameLocationItem) return null
        if (newItem.coverImageUri != coverImageUri) return PayloadPhoto
        return if (newItem.place != place) PayloadLocation
        else Payload.DontUpdate
    }

    interface EditImageListener {
        fun onEditImageClicked(item: Item)
    }

    interface EditLocationListener {
        fun onEditLocationClicked(item: Item)
    }

    companion object {
        const val PayloadLocation = "payloadLocation"
        const val PayloadPhoto = "payloadPhoto"
    }

}