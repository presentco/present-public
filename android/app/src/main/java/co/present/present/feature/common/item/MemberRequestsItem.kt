package co.present.present.feature.common.item

import co.present.present.R
import co.present.present.feature.common.Payload
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_requests.*

class MemberRequestsItem(val numRequests: Int): Item() {

    override fun bind(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(badgeCountPayload)) {
            bind(holder, position)
        } else if (payloads.contains(Payload.DontUpdate)) {
            return
        } else {
            super.bind(holder, position, payloads)
        }
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.badge.text = numRequests.toString()
    }

    override fun getLayout() = R.layout.item_requests

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is MemberRequestsItem
    }

    override fun getChangePayload(newItem: com.xwray.groupie.Item<*>?): Any? {
        if (newItem is MemberRequestsItem && newItem.numRequests != numRequests) return badgeCountPayload
        else return Payload.DontUpdate
    }

    companion object {
        const val badgeCountPayload = "badgeCountPayload"
    }
}