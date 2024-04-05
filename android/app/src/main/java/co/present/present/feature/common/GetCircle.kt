package co.present.present.feature.common

import co.present.present.model.Circle
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable

interface GetCircle {

    fun getCircle(circleId: String): Flowable<Circle>
    fun refreshCircle(circleId: String): Disposable

}