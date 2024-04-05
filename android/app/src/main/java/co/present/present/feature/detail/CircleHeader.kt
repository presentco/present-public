package co.present.present.feature.detail

import android.content.Context
import android.location.Location
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import co.present.present.R
import co.present.present.config.FeatureDataProvider
import co.present.present.extensions.*
import co.present.present.feature.common.item.OnCircleJoinClickListener
import co.present.present.feature.detail.info.OnCircleActionClickListener
import co.present.present.model.Circle
import co.present.present.model.CurrentUser
import kotlinx.android.synthetic.main.header_circle.view.*
import present.proto.GroupMembershipState

class CircleHeader(context: Context?, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    init {
        View.inflate(context, R.layout.header_circle, this)
        fromApi(21) {
            setBackgroundResource(R.color.white)
            elevation = resources.getDimension(R.dimen.toolbar_elevation)
        }
    }

    fun configure(currentUser: CurrentUser?, circle: Circle, location: Location,
                  featureDataProvider: FeatureDataProvider, listener: OnCircleActionClickListener? = null,
                  joinClickListener: OnCircleJoinClickListener? = null) {
        if (circle.joined) {
            join.hide()
            mute.show()
            add.show()

            muteText.setText(if (circle.muted) R.string.unmute else R.string.mute)
            muteIcon.setImageResource(if (circle.muted) R.drawable.ic_muted else R.drawable.ic_mute)
            listOf(muteText, muteIcon).setOnClickListener { listener?.onCircleMuteClicked() }
        } else {
            join.show()
            mute.hide()
            add.hide()

            joinText.setText(if (circle.getGroupMembershipState() == GroupMembershipState.REQUESTED) R.string.requested else R.string.join)
            listOf(joinText, joinIcon).setOnClickListener { joinClickListener?.onCircleJoinClicked(circle, currentUser) }
        }
        listOf(shareText, shareIcon).setOnClickListener { listener?.onCircleShareClicked(circle) }
        listOf(addText, addIcon).setOnClickListener { listener?.onCircleInviteClicked(currentUser) }
        listOf(infoText, infoIcon).setOnClickListener { listener?.onCircleDetailClicked() }

        circleToolbar.configure(circle, location, featureDataProvider)

        coverImage.loadCircleCoverImage(circle)
    }


}