package co.present.present.feature.common

import co.present.present.db.CircleDao
import co.present.present.model.Circle
import co.present.present.service.rpc.CircleService
import co.present.present.service.rpc.getCircle
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers


class GetCircleImpl(val circleDao: CircleDao, val circleService: CircleService): GetCircle {
    override fun refreshCircle(circleId: String): Disposable {
        return circleService.getCircle(circleId)
                .map { circleDao.insert(it) }.toCompletable()
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                        onError = {},
                        onComplete = {}
                )
    }

    // We need this map because getCircle() can be called with different circleIds
    private val map = hashMapOf<String, Flowable<Circle>>()

    override fun getCircle(circleId: String): Flowable<Circle> {
        if (map.containsKey(circleId)) {
            return map.getValue(circleId)
        }

        val circleFlowable = circleDao.getCircle(circleId).replay(1).refCount()
        map[circleId] = circleFlowable

        // On the first subscription, without regard to whether or not the circle is in
        // the database, go fetch an updated copy of it from the network
        refreshCircle(circleId)

        return circleFlowable
    }

}