package co.present.present.feature.common.item

import co.present.present.model.Circle
import co.present.present.model.CurrentUser

interface OnCircleJoinClickListener {
    fun onCircleJoinClicked(circle: Circle, currentUser: CurrentUser?)
}