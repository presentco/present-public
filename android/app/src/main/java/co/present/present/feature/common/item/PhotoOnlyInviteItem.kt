package co.present.present.feature.common.item

import android.net.Uri
import co.present.present.R
import co.present.present.extensions.hide
import co.present.present.extensions.loadCircularImage
import co.present.present.extensions.loadCircularImageFromUri
import co.present.present.extensions.show
import co.present.present.feature.invite.Contact
import co.present.present.feature.invite.getInitials
import co.present.present.model.User
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_contact_photo_only.*

class PhotoOnlyInviteItem(val contact: Contact?, val user: User?) : Item() {
    override fun getLayout() = R.layout.item_contact_photo_only

    override fun bind(viewHolder: ViewHolder, position: Int) {
        if (user != null) {
            viewHolder.initial.hide()
            viewHolder.photo.loadCircularImage(user.photo)
        } else if (contact != null) {
            if (contact.thumbUri != null) {
                viewHolder.photo.loadCircularImageFromUri(Uri.parse(contact.thumbUri))
                viewHolder.initial.hide()
            } else {
                viewHolder.photo.setImageResource(R.drawable.circle_purple)
                viewHolder.initial.show()
                viewHolder.initial.text = contact.getInitials(viewHolder.itemView.context)
            }
        }
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is PhotoOnlyInviteItem && other.contact == contact && other.user == user
    }
}