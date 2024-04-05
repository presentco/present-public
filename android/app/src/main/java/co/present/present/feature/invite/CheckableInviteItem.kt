package co.present.present.feature.invite

import android.content.Context
import android.net.Uri
import co.present.present.R
import co.present.present.extensions.*
import co.present.present.feature.common.Payload
import co.present.present.model.User
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_contact_checkable.*

open class CheckableInviteItem(val contact: Contact?, val user: User?, val isCircleMember: Boolean, val isChecked: Boolean) : Item() {

    constructor(user: User, isCircleMember: Boolean, isChecked: Boolean): this(null, user, isCircleMember, isChecked)

    override fun getLayout() = R.layout.item_contact_checkable

    override fun bind(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(Payload.DontUpdate)) return
        else if (payloads.contains(Payload.Checked)) {
            bindCheck(holder)
            return
        }
        
        bind(holder, position)
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        if (user != null) {
            viewHolder.bindUser(user)
        } else if (contact != null) {
            viewHolder.bindContact(contact)
        }

        viewHolder.friendContainer.isEnabled = !isCircleMember
        viewHolder.checkbox.setVisible(!isCircleMember)
        viewHolder.number.setText(R.string.already_a_member)
        viewHolder.number.setVisible(isCircleMember)
        viewHolder.photo.alpha = if (isCircleMember) .5f else 1f

        bindCheck(viewHolder)
    }

    private fun ViewHolder.bindContact(contact: Contact) {
        name.text = contact.displayName
        if (contact.thumbUri != null) {
            photo.loadCircularImageFromUri(Uri.parse(contact.thumbUri))
            initial.hide()
        } else {
            photo.setImageResource(R.drawable.circle_purple)
            initial.show()
            initial.text = contact.getInitials(itemView.context)
        }
        number.hide()
    }

    private fun ViewHolder.bindUser(user: User) {
        name.text = user.name
        initial.hide()
        number.show()
        photo.loadCircularImage(user.photo)
    }

    private fun bindCheck(viewHolder: ViewHolder) {
        viewHolder.friendContainer.isChecked = isChecked
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is CheckableInviteItem && other.contact == contact
    }

    override fun getChangePayload(newItem: com.xwray.groupie.Item<*>): Any {
        if (newItem is CheckableInviteItem && newItem.isChecked != isChecked) {
            return Payload.Checked
        }
        return Payload.DontUpdate
    }
}

fun CharSequence?.firstInitial(): CharSequence {
    return this?.subSequence(0, 1) ?: ""
}

fun Contact.getInitials(context: Context): String {
    return context.getString(R.string.initials_template, firstName.firstInitial(), lastName.firstInitial())
}