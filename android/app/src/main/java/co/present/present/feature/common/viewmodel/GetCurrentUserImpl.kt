package co.present.present.feature.common.viewmodel

import co.present.present.db.CurrentUserDao
import co.present.present.extensions.Optional
import co.present.present.model.CurrentUser
import io.reactivex.Flowable
import javax.inject.Inject


class GetCurrentUserImpl @Inject constructor(private val currentUserDao: CurrentUserDao) : GetCurrentUser {

    /**
     * This is the canonical and quickest way to get a copy of the current user object.
     *
     * A continuous, live-updating Flowable from the database
     *
     * It won't emit if there's no current user.  If you need it to emit even if there is no user,
     * then use currentUserOptional.
     */
    override val currentUser: Flowable<CurrentUser> by lazy {
        currentUserDao.asFlowable.distinctUntilChanged().replay(1).autoConnect()
    }


    override val currentUserOptional: Flowable<Optional<CurrentUser>> by lazy {
        currentUserDao.optional.replay(1).autoConnect()
    }

}