package co.present.present.feature.common.item

import co.present.present.R
import co.present.present.extensions.hide
import co.present.present.extensions.loadCoverImage
import co.present.present.extensions.show
import co.present.present.feature.common.Payload
import co.present.present.model.Circle
import co.present.present.model.CurrentUser
import co.present.present.view.JoinCircleSelector
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_circle_horizontal.*


open class CircleItemHorizontal(val currentUser: CurrentUser, val circle: Circle, private val listener: OnCircleJoinClickListener? = null) : Item() {

    override fun bind(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(Payload.Joined)){
            bindJoinButton(holder)
        } else {
            super.bind(holder, position, payloads)
        }
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        with(viewHolder) {
            title.text = circle.title
            neighborhood.text = circle.locationName
            coverImage.loadCoverImage(circle)
        }
    }

    private fun bindJoinButton(viewHolder: ViewHolder) {
        with(viewHolder) {
            if (listener == null) {
                // If no join listener, hide the button entirely.
                joinButton.hide()
            } else {
                joinButton.show()
                joinButton.setImageDrawable(JoinCircleSelector(viewHolder.itemView.context))
                joinButton.isSelected = circle.joined
                joinButton.setOnClickListener {
                    listener.onCircleJoinClicked(circle, currentUser)
                }
            }
        }
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>): Boolean {
        return other is CircleItemHorizontal && other.circle.id == circle.id
    }

    override fun getChangePayload(newItem: com.xwray.groupie.Item<*>?): Any {
        val other = newItem as CircleItemHorizontal
        if (circle.joined != other.circle.joined) return Payload.Joined
        return Payload.DontUpdate
    }

    override fun getLayout(): Int = R.layout.item_circle_horizontal

}

class CircleItemPreview(currentUser: CurrentUser, circle: Circle, private val listener: OnCircleJoinClickListener? = null): CircleItemHorizontal(currentUser, circle, listener) {

    override fun getLayout() = R.layout.item_circle_preview
}
