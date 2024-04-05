package co.present.present.feature.common.item

import android.view.View
import co.present.present.R
import co.present.present.extensions.loadCircularImage
import co.present.present.feature.common.Payload
import co.present.present.model.Circle
import co.present.present.model.User
import co.present.present.model.isOwner
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_friend.*


open class FriendItem(val user: User) : Item() {
    override fun getLayout() = R.layout.item_friend

    override fun bind(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(Payload.DontUpdate)) return
        super.bind(holder, position, payloads)
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.name.text = user.firstName
        viewHolder.photo.loadCircularImage(user.photo)
        viewHolder.owner.visibility = View.GONE
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is FriendItem && other.user.id == user.id
    }

    override fun getChangePayload(newItem: com.xwray.groupie.Item<*>?): Any {
        return Payload.DontUpdate
    }
}

open class MemberItem(user: User, val circle: Circle) : FriendItem(user) {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        super.bind(viewHolder, position)
        viewHolder.owner.visibility = if (user.isOwner(circle)) View.VISIBLE else View.GONE
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is MemberItem && other.user.id == user.id
    }
}

