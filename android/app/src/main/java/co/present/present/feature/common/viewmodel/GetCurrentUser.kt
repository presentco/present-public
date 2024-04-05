package co.present.present.feature.common.viewmodel

import co.present.present.extensions.Optional
import co.present.present.model.CurrentUser
import io.reactivex.Flowable

interface GetCurrentUser {

    val currentUser: Flowable<CurrentUser>

    val currentUserOptional: Flowable<Optional<CurrentUser>>

}