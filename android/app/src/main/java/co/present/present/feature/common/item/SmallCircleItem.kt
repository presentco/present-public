package co.present.present.feature.common.item

import co.present.present.R
import co.present.present.extensions.hide
import co.present.present.extensions.loadCircleCoverImage
import co.present.present.extensions.setVisible
import co.present.present.extensions.show
import co.present.present.feature.common.Payload
import co.present.present.model.Circle
import co.present.present.model.CurrentUser
import co.present.present.model.isOwner
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import getXShortRelativeDate
import kotlinx.android.synthetic.main.item_circle_small.*
import toLocalDateTime

/**
 * View holder for cards representing a Present discussion group (circle).
 */
data class SmallCircleItem(val currentUser: CurrentUser? = null,
                           val circle: Circle,
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
            bindJoin(viewHolder)
            coverImage.loadCircleCoverImage(circle)

            commentsCount.text = circle.commentCount.toString()
            arrayOf(commentsIcon, commentsCount).forEach { it.setVisible(circle.hasComments()) }

            membersCount.text = circle.participantCount.toString()
            arrayOf(membersIcon, membersCount).forEach { it.setVisible(circle.hasMembers()) }

            neighborhood.text = circle.locationName

            if (circle.joined && listener == null) {
                elapsedTime.show()
                val date = elapsedTime.context.getXShortRelativeDate(circle.lastCommentTime.toLocalDateTime())
                elapsedTime.hint = date // Stupid text measuring CL bug
                elapsedTime.text = date
                badge.setVisible(circle.unread)
                badge.text = getBadgeCount().toString()
            } else {
                elapsedTime.hide()
                badge.hide()
            }
        }
    }

    private fun getBadgeCount(): Int {
        val count = circle.unreadCount + (if (currentUser?.isOwner(circle) == true) circle.joinRequests else 0)
        if (count == 0 && circle.unread) return 1 else return count
    }

    private fun bindJoin(viewHolder: ViewHolder) {
        viewHolder.joinButton.bindJoin(circle, currentUser, this, listener)
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>): Boolean {
        return other is SmallCircleItem && other.circle.id == circle.id
    }

    override fun getChangePayload(newItem: com.xwray.groupie.Item<*>?): Any {
        val other = newItem as SmallCircleItem
        return if (circle.membershipState != other.circle.membershipState) Payload.Joined else Payload.DontUpdate
    }

    override fun getLayout(): Int = R.layout.item_circle_small

}
