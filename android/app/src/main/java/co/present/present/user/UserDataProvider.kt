package co.present.present.user

import android.content.SharedPreferences
import co.present.present.db.Database
import co.present.present.extensions.preferences
import co.present.present.feature.onboarding.listeners.UserLoginListener
import co.present.present.model.CurrentUser
import co.present.present.model.Interest
import co.present.present.model.toInterest
import co.present.present.service.RpcManager
import co.present.present.service.rpc.putToken
import co.present.present.service.rpc.synchronize
import com.jakewharton.rx.ReplayingShare
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import present.proto.*
import javax.inject.Inject


/**
 * This class started life as a sort of God object managing everything to do with user profiles.
 * Over time, I hope to take all database and network access out of it and move them into the new
 * ViewModels and leave this as a shim for current-user-related SharedPreferences.
 */
class UserDataProvider @Inject
constructor(private val db: Database,
            private val rpcManager: RpcManager,
            prefs: SharedPreferences) : UserLoginListener {
    private val TAG: String = javaClass.simpleName

    // TODO: We should not hold this here
    @get:Deprecated("Use currentUserLocalMaybe() instead")
    var userProfile: CurrentUser? = null

    var firebaseToken: String? by preferences(prefs, null)
    var tokenUploaded: Boolean by preferences(prefs, false)
    var firstName: String? by preferences(prefs, null)
    var lastName: String? by preferences(prefs, null)
    var bio: String? by preferences(prefs, null)
    var photoUuid: String? by preferences(prefs, null)
    var interests: Collection<Interest> by preferences<Set<String>, Collection<Interest>>(prefs, setOf(),
            { interests -> interests.map { it.canonicalString }.toSet() },
            { strings -> strings.mapNotNull { it.toInterest() } })
    var userNextStep: Authorization.NextStep by preferences(prefs, Authorization.NextStep.AUTHENTICATE, { it.name },
            { Authorization.NextStep.valueOf(it) })
    var blockMessage: String? by preferences(prefs, null)
    var features: List<Feature> by preferences<Set<String>, List<Feature>>(prefs, listOf(),
            { features -> features.map { it.value.toString() }.toSet() },
            { strings -> strings.mapNotNull { Feature.fromValue(it.toInt()) } })

    private val userService: UserService by lazy {
        rpcManager.getService(UserService::class.java)
    }

    val currentUserLocalMaybe: Maybe<CurrentUser>
        get() = db.currentUserDao().get().doOnSuccess {
            userProfile -> this.userProfile = userProfile
        }

    /**
     * This won't emit if there's no current user.  If you need it to complete even if there is no user,
     * then use currentUserLocalMaybe() which returns the first value as a Maybe.
     */
    @get:Deprecated("Use a ViewModel that delegates to GetCurrentUser")
    val currentUserLocal: Flowable<CurrentUser> by lazy {
        db.currentUserDao().asFlowable
                // TODO: We should NOT be caching this here (but parts of onboarding still rely on it)
                .doOnNext { userProfile -> this.userProfile = userProfile }
                // Caches the last value only and replays it for new subscribers (so we only hit the
                // database once per unique value no matter how many subscribers)
                .compose(ReplayingShare.instance())
    }

    override fun onLoginSuccess(authorizationResponse: AuthorizationResponse) {
        persistAuthResponse(authorizationResponse)
    }

    override fun onFacebookLinkSuccess(authorizationResponse: AuthorizationResponse) {
        persistAuthResponse(authorizationResponse)
    }

    private fun persistSyncResponse(synchronizeResponse: SynchronizeResponse): Single<SynchronizeResponse> {
        return db.persistUserProfile(synchronizeResponse.userProfile).toSingleDefault(true)
                .map {
                    userNextStep = synchronizeResponse.authorization.nextStep
                    blockMessage = synchronizeResponse.authorization.blockScreen?.text
                    features = synchronizeResponse.features
                    synchronizeResponse
                }
    }

    fun persistAuthResponseRx(authorizationResponse: AuthorizationResponse): Single<AuthorizationResponse> {
        return db.persistUserProfile(authorizationResponse.userProfile).toSingleDefault(authorizationResponse)
                .map {
                    userNextStep = it.authorization.nextStep
                    blockMessage = it.authorization.blockScreen?.text
                    it
                }
    }

    private fun persistAuthResponse(authorizationResponse: AuthorizationResponse) {
        db.persistUserProfileAsync(authorizationResponse.userProfile)
        userNextStep = authorizationResponse.authorization.nextStep
        blockMessage = authorizationResponse.authorization.blockScreen?.text
    }

    fun synchronize(): Single<SynchronizeResponse> {
        return userService.synchronize(true)
                .flatMap { persistSyncResponse(it) }
    }

    fun putDeviceToken(token: String): Completable {
        return userService.putToken(token)
    }

    fun clear() {
        firstName = null
        lastName = null
        bio = null
        photoUuid = null
        interests = setOf()
        userNextStep = Authorization.NextStep.AUTHENTICATE
        blockMessage = null
        tokenUploaded = false
    }
}
