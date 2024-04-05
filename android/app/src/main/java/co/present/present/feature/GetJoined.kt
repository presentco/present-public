package co.present.present.feature

import co.present.present.db.CircleDao
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.model.Circle
import co.present.present.service.rpc.CircleService
import co.present.present.service.rpc.getJoinedCircles
import io.reactivex.Flowable


interface GetJoined {
    fun getJoinedCircles(userId: String): Flowable<List<Circle>>
}

class GetJoinedImpl(val getCurrentUser: GetCurrentUser, val circleService: CircleService, val circleDao: CircleDao) : GetJoined {

    private val TAG = javaClass.simpleName

    override fun getJoinedCircles(userId: String): Flowable<List<Circle>> {
        return getCurrentUser.currentUser.flatMap { currentUser ->
            if (userId == currentUser.id) {
                // If this is the current user, use what's already in the DB
                circleDao.getJoinedCirclesByLastActivity()
            } else {
                // Otherwise fetch from network, and serve updates from database.
                // Return empty list first so the page doesn't wait on the network call to render.
                // Because we don't know the ids of the circles the user has joined until we make the
                // network call, we can't await database results until the network finishes.
                circleService.getJoinedCircles(userId).map { joinedCircles ->
                    circleDao.insertCircles(joinedCircles); joinedCircles
                }.toFlowable().flatMap { joinedCircles -> circleDao.getJoinedCircles(joinedCircles.map { it.id }) }
                        .startWith(Flowable.just(emptyList()))
            }
        }
    }

}