package co.present.present.feature.onboarding.step

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.VisibleForTesting
import co.present.present.BaseActivity
import co.present.present.LaunchActivity
import co.present.present.R
import co.present.present.analytics.AmplitudeEvents
import co.present.present.extensions.hide
import co.present.present.extensions.slideOverFromRight
import co.present.present.extensions.start
import co.present.present.extensions.toast
import co.present.present.feature.onboarding.OnboardingActivity
import co.present.present.feature.onboarding.WaitlistActivity
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.squareup.otto.Bus
import dagger.Lazy
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_facebook_login.*
import present.proto.AuthorizationResponse
import javax.inject.Inject


open class FacebookLinkActivity : BaseActivity() {
    private val TAG = FacebookLinkActivity::class.java.simpleName

    private val permissions = arrayListOf("public_profile", "email", "user_friends")
    @Inject lateinit var bus: Bus
    @Inject lateinit var loginManager: LoginManager
    @Inject lateinit var callbackManager: CallbackManager
    @VisibleForTesting
    @Inject lateinit open var facebookIdProvider: Lazy<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_facebook_login)

        loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {

            override fun onSuccess(loginResult: LoginResult) {
                Log.d(TAG, "onSuccess()")
                analytics.log(AmplitudeEvents.FACEBOOK_CONNECT_SUCCESS)
                val token = loginResult.accessToken.token
                if (facebookIdProvider.get() == null) {
                    // Happens if the Facebook app isn't installed.
                    // DON'T DELETE this code path. I can't figure out how to test it, but it's important!
                    object : ProfileTracker() {
                        override fun onCurrentProfileChanged(oldProfile: Profile?, currentProfile: Profile) {
                            stopTracking()
                            linkFacebook(token, currentProfile.id)
                        }
                    }
                } else {
                    linkFacebook(token, facebookIdProvider.get())
                }
            }

            override fun onCancel() {
                progress.hide()
                analytics.log(AmplitudeEvents.FACEBOOK_CONNECT_CANCEL)
                Log.d(TAG, "onCancel()")
                finish()
            }

            override fun onError(e: FacebookException) {
                progress.hide()
                if (e is FacebookAuthorizationException && e.message.isNetworkError()) {
                    toast(R.string.facebook_network_error)
                }
                Log.e(TAG, "onError()", e)
                analytics.log(AmplitudeEvents.FACEBOOK_CONNECT_ERROR)
                finish()
            }
        })

        loginManager.logInWithReadPermissions(this, permissions)
        analytics.log(AmplitudeEvents.FACEBOOK_CONNECT_VIEW)
    }

    fun CharSequence?.isNetworkError(): Boolean {
        return this != null && (contains("ERR_INTERNET_DISCONNECTED") || contains("CONNECTION_FAILURE"))
    }

    private fun linkFacebook(token: String, uuid: String) {
        onboardingDataProvider.linkFacebook(token, uuid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        onNext = { handleAuthorizationResponse(it) },
                        onError = { e ->
                            progress.hide()
                            Log.e(TAG, "Error linking Facebook credentials with Present server", e)
                            toast(R.string.generic_error)
                            finish()
                        }
                )
    }

    override fun performInjection() {
        activityComponent.inject(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleAuthorizationResponse(authorizationResponse: AuthorizationResponse) {
        Log.d(TAG, "Authorization response, next step: ${authorizationResponse.authorization.nextStep}")
        start<LaunchActivity>()
        finish()
    }

    open fun onUserWaitlisted() {
        start<WaitlistActivity>()
        supportFinishAfterTransition()
    }

    open fun onUserCreated() {
        slideOverFromRight<OnboardingActivity>()
        supportFinishAfterTransition()
    }
    
}
