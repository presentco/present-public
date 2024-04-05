package co.present.present.feature.onboarding.step

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import co.present.present.BaseActivity
import co.present.present.LaunchActivity
import co.present.present.R
import co.present.present.analytics.AmplitudeEvents
import co.present.present.extensions.*
import co.present.present.feature.onboarding.OnboardingActivity
import co.present.present.feature.onboarding.PhoneLoginActivity
import co.present.present.feature.onboarding.WaitlistActivity
import com.squareup.otto.Bus
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_link_verify.*
import present.proto.Authorization
import present.proto.AuthorizationResponse
import javax.inject.Inject


open class LinkVerificationActivity : BaseActivity() {
    private val TAG = LinkVerificationActivity::class.java.simpleName

    @Inject lateinit var bus: Bus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_link_verify)
        bus.register(this)
        analytics.log(AmplitudeEvents.APP_AUTHORIZE_WITH_MAGIC_LINK)

        facebookButton.setOnClickListener {
            start<PhoneLoginActivity>()
            finish()
        }

        retryButton.setOnClickListener{
            verifyMagicLink()
        }
    }

    override fun onResume() {
        super.onResume()
        verifyMagicLink()
    }

    private fun verifyMagicLink() {
        onboardingDataProvider.verify(intent.data)
                .compose(applySingleSchedulers())
                .doOnSubscribe {
                    spinner.show()
                    errorView.hide()
                }
                .subscribeBy(
                        onSuccess = {
                            analytics.log(AmplitudeEvents.APP_AUTHORIZE_WITH_MAGIC_LINK_SUCCEEDED)
                            handleAuthorizationResponse(it)
                        },
                        onError = { e ->
                            analytics.log(AmplitudeEvents.APP_AUTHORIZE_WITH_MAGIC_LINK_FAILED)
                            Log.e(TAG, "Error verifying email link with Present server", e)
                            toast(R.string.generic_error)
                            spinner.hide()
                            errorView.show()
                        }
                )
    }

    override fun performInjection() {
        activityComponent.inject(this)
    }

    private fun handleAuthorizationResponse(authorizationResponse: AuthorizationResponse) {
        when (authorizationResponse.authorization.nextStep) {
            Authorization.NextStep.PROCEED -> {
                Log.d(TAG, "Email linking succeeded and user already exists; proceed to app")
                onUserLoggedIn()
            }
            Authorization.NextStep.SIGN_UP -> {
                Log.d(TAG, "Email linking succeeded; please complete sign up")
                onUserCreated()
            }
            else -> {
                Log.e(TAG, "Email linking failed, next step: " + authorizationResponse.authorization.nextStep)
                onUserWaitlisted()
            }
        }
    }

    private fun onUserLoggedIn() {
        start<LaunchActivity>()
        finish()
    }

    private fun onUserWaitlisted() {
        startWithSharedElement(WaitlistActivity::class.java, wordmark)
        supportFinishAfterTransition()
    }

    private fun onUserCreated() {
        slideOverFromRight<OnboardingActivity>()
        supportFinishAfterTransition()
    }

    override fun onDestroy() {
        bus.unregister(this)
        super.onDestroy()
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    companion object {
        fun newIntent(context: Context, uri: Uri): Intent {
            return Intent(context, LinkVerificationActivity::class.java).apply {
                data = uri
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }
    }
}
