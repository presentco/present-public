package co.present.present.feature.onboarding

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import co.present.present.di.ActivityScope
import co.present.present.extensions.preferences
import co.present.present.feature.onboarding.OnboardingActivity.Step
import co.present.present.location.LocationDataProvider
import co.present.present.service.RpcManager
import co.present.present.service.rpc.completeSignup
import co.present.present.service.rpc.linkFacebook
import co.present.present.service.rpc.verifyLink
import co.present.present.user.UserDataProvider
import com.squareup.otto.Bus
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import present.proto.AuthorizationResponse
import present.proto.UserService
import javax.inject.Inject

@ActivityScope
class OnboardingDataProvider @Inject
constructor(private val rpcManager: RpcManager, val locationDataProvider: LocationDataProvider, private val userDataProvider: UserDataProvider,
            private val bus: Bus, private val prefs: SharedPreferences, val application: Application) {
    private val TAG: String = javaClass.simpleName

    private val userService: UserService by lazy {
        rpcManager.getService(UserService::class.java)
    }

    var currentStep: Step by preferences(prefs, Step.PhotoAndName, { it.name }, { Step.valueOf(it) })

    var onboardingCompleted: Boolean by preferences(prefs, false)

    fun linkFacebook(token: String, uuid: String): Observable<AuthorizationResponse> {
        return userService.linkFacebook(token, uuid)
                .flatMap { userDataProvider.persistAuthResponseRx(it).toObservable() }
    }

    fun verify(uri: Uri): Single<AuthorizationResponse> {
        return userService.verifyLink(uri).flatMap { userDataProvider.persistAuthResponseRx(it) }
    }

    fun completeSignup(): Single<AuthorizationResponse> {
        return locationDataProvider.getLocation(application.applicationContext)
                .flatMap { userService.completeSignup().subscribeOn(Schedulers.io()) }
                .doOnSuccess { userDataProvider.onLoginSuccess(it) }
    }


    fun clear() {
        currentStep = Step.PhotoAndName
        onboardingCompleted = false
    }
}

