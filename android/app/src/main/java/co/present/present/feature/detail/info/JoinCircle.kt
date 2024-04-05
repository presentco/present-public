package co.present.present.feature.detail.info

import co.present.present.model.Circle
import io.reactivex.Completable


interface JoinCircle {
    fun toggleCircleJoin(circle: Circle): Completable
}