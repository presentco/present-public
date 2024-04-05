package co.present.present.feature.onboarding

import android.app.Activity
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import co.present.present.BaseActivity
import co.present.present.R
import co.present.present.analytics.AmplitudeEvents
import co.present.present.extensions.*
import co.present.present.feature.invite.OnboardingAddFriendsActivity
import co.present.present.feature.onboarding.events.AdvanceOnboardingEvent
import co.present.present.feature.onboarding.events.AppSettingsRequestedEvent
import co.present.present.feature.onboarding.step.ConfirmNameAndPhotoFragment
import co.present.present.feature.onboarding.step.LocationPermissionFragment
import co.present.present.feature.profile.EditProfileViewModel
import co.present.present.location.LocationPermissions
import co.present.present.model.CurrentUser
import com.squareup.otto.Bus
import com.squareup.otto.Subscribe
import io.reactivex.rxkotlin.subscribeBy
import present.proto.Authorization
import present.proto.AuthorizationResponse
import javax.inject.Inject



open class OnboardingActivity : BaseActivity() {
    private val TAG = javaClass.simpleName
    @Inject lateinit var bus: Bus
    @Inject lateinit var locationPermissions: LocationPermissions

    private lateinit var viewModel: EditProfileViewModel

    enum class Step {
        PhotoAndName {
            override fun isRequired(currentUser: CurrentUser, activity: Activity, locationPermissions: LocationPermissions): Boolean {
                return currentUser.name.first.isBlank() || currentUser.name.last.isBlank()
            }
        },
        Location {
            override fun isRequired(currentUser: CurrentUser, activity: Activity, locationPermissions: LocationPermissions): Boolean {
                return SDK_INT >= M && !locationPermissions.isGranted(activity)
            }
        };

        operator fun inc(): Step {
            val position = Step.values().indexOf(this)
            return Step.values()[position + 1]
        }

        abstract fun isRequired(currentUser: CurrentUser, activity: Activity, locationPermissions: LocationPermissions): Boolean
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(EditProfileViewModel::class.java)
        analytics.log(AmplitudeEvents.SIGNUP_START)
        showStep()
    }

    override fun onResume() {
        super.onResume()
        bus.register(this)
    }

    @Subscribe
    fun advance(event: AdvanceOnboardingEvent) {
        if (onboardingDataProvider.currentStep == Step.values().last()) {
            onboardingDataProvider.completeSignup()
                    .compose(applySingleSchedulers())
                    .subscribeBy(
                    onSuccess = { handleSignupResponse(it) },
                    onError = { e ->
                        Log.e(TAG, "Error completing signup", e)
                        dialog(R.string.network_error, positiveButtonText = R.string.ok)
                    }
            )
        } else {
            onboardingDataProvider.currentStep++
            showStep()
        }
    }

    private fun handleSignupResponse(response: AuthorizationResponse) {
        onboardingDataProvider.onboardingCompleted = true

        when (response.authorization.nextStep) {
            Authorization.NextStep.PROCEED -> {
                Log.d(TAG, "Signup completed and user logged in successfully")
                onUserLoggedIn()
            }
            else -> {
                Log.e(TAG, "Reached server but can't complete signup, next step: " + response.authorization.nextStep)
                onUserWaitlist()
            }
        }
    }

    private fun showStep() {
        viewModel.currentUser.firstOrError()
                .compose(applySingleSchedulers())
                .subscribeBy(
                        onSuccess = { currentUser -> showStepIfRequired(currentUser) },
                        onError = { e ->
                            Log.e(TAG, "Error fetching current user profile, this should never happen", e)
                        }
                )
    }

    private fun showStepIfRequired(currentUser: CurrentUser) {
        if (!onboardingDataProvider.currentStep.isRequired(currentUser, this, locationPermissions)) {
            Log.d(TAG, "Skipping step ${onboardingDataProvider.currentStep} because it is not required")
            advance(AdvanceOnboardingEvent())
            return
        }
        Log.d(TAG, "Showing onboarding step: ${onboardingDataProvider.currentStep}")
        val fragment = getNewFragment(onboardingDataProvider.currentStep)
        slideOverFromRight(fragment, R.id.frame)
    }

    private fun getNewFragment(step: Step): Fragment {
        return when (step) {
            Step.PhotoAndName -> ConfirmNameAndPhotoFragment()
            Step.Location -> LocationPermissionFragment()
        }
    }

    @Subscribe
    fun onAppSettingsRequested(event: AppSettingsRequestedEvent) {
        showApplicationSettings()
    }

    private fun onUserLoggedIn() {
        slideFromRight<OnboardingAddFriendsActivity>()
        analytics.log(AmplitudeEvents.SIGNUP_COMPLETE)
        finish()
    }

    fun onUserWaitlist() {
        slideFromRight<WaitlistActivity>()
        analytics.log(AmplitudeEvents.SIGNUP_BLOCKED)
        supportFinishAfterTransition()
    }

    public override fun onPause() {
        super.onPause()
        bus.unregister(this)
    }

    override fun performInjection() {
        activityComponent.inject(this)
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }
}

