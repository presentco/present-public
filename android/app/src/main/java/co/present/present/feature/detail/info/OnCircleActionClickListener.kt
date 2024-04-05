package co.present.present.feature.detail.info

import co.present.present.model.Circle
import co.present.present.model.CurrentUser

interface OnCircleActionClickListener {
    fun onCircleShareClicked(circle: Circle)
    fun onCircleInviteClicked(currentUser: CurrentUser?)
    fun onCircleDetailClicked()
    fun onCircleMuteClicked()
}