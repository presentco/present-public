package co.present.present

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import co.present.present.config.FeatureDataProvider
import co.present.present.extensions.Optional
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.feature.profile.GetBlocked
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import present.proto.Gender
import present.proto.UserService
import javax.inject.Inject

class DebugViewModel @Inject constructor(application: Application, getBlocked: GetBlocked, getCurrentUser: GetCurrentUser,
                                         val userService: UserService, val featureDataProvider: FeatureDataProvider) : AndroidViewModel(application),
        GetBlocked by getBlocked, GetCurrentUser by getCurrentUser {

    fun getBlockListTitle(): Flowable<Optional<String>> {
        return currentUserOptional.combineLatest(getBlocked()).map { (currentUserOptional, blocked) ->
            if (currentUserOptional.value == null) Optional<String>(null)
            else if (blocked.isNotEmpty()) Optional("Clear blocked users: ${blocked.size}")
            else Optional("No blocked users")
        }
    }

    fun clearBlockList(): Completable {
        return getBlocked().flatMapCompletable {
            Flowable.fromIterable(it).flatMapCompletable {
                toggleUserBlock(it.userId, true)
            }.subscribeOn(Schedulers.newThread())
        }
    }

    private val genderOverrideChanged = BehaviorSubject.createDefault(featureDataProvider.overrideGender)

    fun getGenderTitle(): Flowable<Optional<String>> {
        return currentUserOptional.combineLatest(genderOverrideChanged.toFlowable(BackpressureStrategy.LATEST))
                .map { (currentUserOptional, genderOverride) ->
            val currentUser = currentUserOptional.value
            Optional(
                    if (currentUser == null) null
                    else if (genderOverride == -1) {
                        "Gender: Default (${Gender.values()[currentUser.gender ?: 0]})"
                    } else {
                        "Gender: ${Gender.values()[genderOverride]}"
                    })
        }
    }

    fun setGenderOverride(selectedGender: Int) {
        featureDataProvider.overrideGender = selectedGender
        genderOverrideChanged.onNext(selectedGender)
    }
}