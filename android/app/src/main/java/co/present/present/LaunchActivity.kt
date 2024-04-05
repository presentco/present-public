package co.present.present

import android.content.Intent
import android.os.Bundle
import android.util.Log
import co.present.present.analytics.AmplitudeEvents
import co.present.present.di.ActivityScope
import co.present.present.extensions.applySingleSchedulers
import co.present.present.extensions.isVerifyLink
import co.present.present.extensions.start
import co.present.present.feature.MainActivity
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.feature.onboarding.OnboardingActivity
import co.present.present.feature.onboarding.PhoneLoginActivity
import co.present.present.feature.onboarding.WaitlistActivity
import co.present.present.feature.onboarding.step.LinkVerificationActivity
import co.present.present.location.LocationPermissionActivity
import co.present.present.location.LocationPermissions
import co.present.present.model.CurrentUser
import io.reactivex.rxkotlin.subscribeBy
import present.proto.Authorization
import javax.inject.Inject




/**
 * Splash screen for initial launch of app, which redirects to the appropriate
 * flow based on whether or not the user is already logged in.
 */
@ActivityScope
open class LaunchActivity : BaseActivity() {
    private val TAG = javaClass.simpleName
    @Inject lateinit var locationPermissions: LocationPermissions
    @Inject lateinit var getCurrentUser: GetCurrentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)
        analytics.initialize(this)
        analytics.log(AmplitudeEvents.APP_LAUNCHED)
        analytics.log(AmplitudeEvents.APP_VIEW_SPLASH_SCREEN)
    }

    override fun onResume() {
        super.onResume()
        getCurrentUser.currentUserOptional.firstOrError()
                .compose(applySingleSchedulers())
                .subscribeBy(
                        onError = { Log.e(TAG, "Error getting current user optional from DB", it) },
                        onSuccess = { chooseActivity(it.value) }
                )
    }

    private fun chooseActivity(currentUser: CurrentUser?) {

        when (userDataProvider.userNextStep) {
            Authorization.NextStep.AUTHENTICATE -> {
                Log.d(TAG, "AUTHENTICATE: We've either never talked to server before, or it says to authenticate. Proceeding to login")
                if (intent?.data?.isVerifyLink() == true) {
                    start(LinkVerificationActivity.newIntent(this, intent.data))
                } else if (currentUser != null) {
                    Log.d(TAG, "Forcing user to phone login -- they were probably a previous FB login.")
                    start<PhoneLoginActivity>()
                } else {
                    Log.d(TAG, "Launching main activity for preview mode.")
                    startMainActivity()
                }
            }
            Authorization.NextStep.BLOCK -> {
                Log.d(TAG, "BLOCK: User is blocked, showing waitlist")
                start<WaitlistActivity>()
            }
            Authorization.NextStep.SIGN_UP -> {
                Log.d(TAG, "SIGN_UP: User is initialized but not finalized, resuming onboarding")
                start<OnboardingActivity>()
            }
            Authorization.NextStep.PROCEED -> {
                Log.d(TAG, "PROCEED: User is fully created and we're allowed to proceed")

                if (!locationPermissions.isGranted(this)) {
                    Log.d(TAG, "... but location is disabled locally. Prompting for location permission")
                    start<LocationPermissionActivity>()
                } else {
                    startMainActivity()
                }
            }
        }

        supportFinishAfterTransition()
    }

    private fun startMainActivity() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            if (intent?.data?.isVerifyLink() == true) {
                // Don't forward verification links -- this is the only place we should
                // see or handle them.
            } else {
                // Whatever Intent this activity was started with, relay it to the main activity for handling
                action = intent.action
                data = intent.data
                putExtras(intent)
                Log.d(TAG, "Relaying launch intent to main activity: action: ${intent.action} data: ${intent.data} extras: ${intent.extras}")
            }
        }
        start(launchIntent)
    }

    override fun performInjection() {
        activityComponent.inject(this)
    }
}
