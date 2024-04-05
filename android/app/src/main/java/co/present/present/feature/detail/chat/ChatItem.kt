package co.present.present.feature.detail.chat

import android.view.View
import co.present.present.R
import co.present.present.extensions.hide
import co.present.present.extensions.load
import co.present.present.extensions.loadCircularImage
import co.present.present.extensions.show
import co.present.present.model.Chat
import co.present.present.view.LinkTouchMovementMethod
import co.present.present.view.OnLinkClickedListener
import co.present.present.view.replaceSpans
import com.bumptech.glide.Glide
import com.xwray.groupie.OnItemClickListener
import com.xwray.groupie.OnItemLongClickListener
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.item_chat.*
import timeFormat

/**
 * Model for a comment in a circle view.
 */
class ChatItem(val chat: Chat, private val onLinkClickedListener: OnLinkClickedListener, private val photoClickedListener: OnPhotoClickedListener, private val userClickedListener: OnUserClickedListener) : Item() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        // In order to deal directly with the onItemLongClickListener, doing all binding in
        // the longer override version of this method
    }

    override fun getLayout() = R.layout.item_chat

    override fun bind(viewHolder: ViewHolder, position: Int, payloads: MutableList<Any>, onItemClickListener: OnItemClickListener?, onItemLongClickListener: OnItemLongClickListener?) {
        super.bind(viewHolder, position, payloads, onItemClickListener, onItemLongClickListener)

        viewHolder.photo.loadCircularImage(chat.user.avatar)
        viewHolder.name.text = chat.user.name

        chat.comment.trim().let {
            viewHolder.message.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
            viewHolder.message.text = it
        }

        viewHolder.message.replaceSpans(onLinkClickedListener)
        viewHolder.message.movementMethod = LinkTouchMovementMethod.getInstance()

        viewHolder.time.text = timeFormat.format(chat.createdAt)
        if (chat.photo == null) {
            Glide.clear(viewHolder.image)
            viewHolder.imageContainer.hide()
        } else {
            viewHolder.imageContainer.show()
            viewHolder.imageContainer.post {
                loadImage(viewHolder)
            }
        }

        viewHolder.image.setOnClickListener(if (chat.photo == null) null else View.OnClickListener {
            photoClickedListener.onPhotoClicked(this@ChatItem)
        })

        viewHolder.photo.setOnClickListener {
            userClickedListener.onUserClicked(this@ChatItem)
        }

        // Without these overrides, the photo and profile image swallow all touch events and the
        // long click doesn't get propagated to the parent.
        viewHolder.photo.setOnLongClickListener { viewHolder.itemView.performLongClick() }
        viewHolder.image.setOnLongClickListener { viewHolder.itemView.performLongClick() }

    }

    private fun loadImage(viewHolder: ViewHolder) {
        val cornerRadius = viewHolder.itemView.context.resources.getDimensionPixelSize(R.dimen.image_corner_radius)
        viewHolder.image.load(
                chat.photo,
                bitmapTransform = RoundedCornersTransformation(viewHolder.itemView.context, cornerRadius, 0),
                placeholder = R.drawable.rounded_corners_light_gray_8dp)
    }

    interface OnPhotoClickedListener {
        fun onPhotoClicked(item: ChatItem)
    }

    interface OnUserClickedListener {
        fun onUserClicked(item: ChatItem)
    }

}
