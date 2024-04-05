package co.present.present.feature.common.item

import android.location.Location
import android.view.View
import co.present.present.R
import co.present.present.extensions.hide
import co.present.present.extensions.loadCoverImage
import co.present.present.extensions.show
import co.present.present.feature.common.Payload
import co.present.present.location.formattedDistanceTo
import co.present.present.model.Circle
import co.present.present.model.CurrentUser
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import getShortDateTimeRange
import kotlinx.android.synthetic.main.item_circle.*

/**
 * View holder for cards representing a Present discussion group (circle).
 */
data class CircleItem(val currentUser: CurrentUser? = null, val circle: Circle, val location: Location? = null,
                      private val listener: OnCircleJoinClickListener? = null) : Item() {

    override fun bind(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(Payload.Joined)) {
            bindJoin(holder)
        } else {
            super.bind(holder, position, payloads)
        }
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        with(viewHolder) {
            title.text = circle.title
            neighborhood.text = circle.locationName

            location?.apply {
                distance.text = formattedDistanceTo(circle.location)
            }
            bindJoin(viewHolder)
            coverImage.loadCoverImage(circle)
            badge.visibility = if (circle.unread) View.VISIBLE else View.INVISIBLE

            if (circle.startTime == null) {
                time.hide()
                timeIcon.hide()
            } else {
                time.text = getShortDateTimeRange(circle.startTime, circle.endTime ?: circle.startTime)
                time.show()
                timeIcon.show()
            }
        }
    }

    private fun bindJoin(viewHolder: ViewHolder) {
        viewHolder.joinButton.bindJoin(circle, currentUser, this, listener)
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>): Boolean {
        return other is CircleItem && other.circle.id == circle.id
    }

    override fun getChangePayload(newItem: com.xwray.groupie.Item<*>?): Any {
        val other = newItem as CircleItem
        return if (circle.membershipState != other.circle.membershipState) Payload.Joined else Payload.DontUpdate
    }

    override fun getLayout(): Int = R.layout.item_circle

}
