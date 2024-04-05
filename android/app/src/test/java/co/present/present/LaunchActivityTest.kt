package co.present.present


import android.annotation.SuppressLint
import co.present.present.analytics.AmplitudeEvents
import co.present.present.analytics.Analytics
import co.present.present.config.FeatureDataProvider
import co.present.present.feature.onboarding.OnboardingActivity
import co.present.present.feature.onboarding.OnboardingDataProvider
import co.present.present.model.CurrentUser
import co.present.present.user.UserDataProvider
import io.reactivex.Maybe
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import present.proto.Authorization

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class LaunchActivityTest {
    private lateinit var activity: LaunchActivityStub
    @Mock lateinit var userProfile: CurrentUser
    private lateinit var maybe: Maybe<CurrentUser>
    private lateinit var activityController: ActivityController<LaunchActivityStub>

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        maybe = Maybe.just(userProfile)
        activityController = Robolectric.buildActivity(LaunchActivityStub::class.java).create()
        activity = activityController.get()

        `when`(activity.userDataProvider.currentUserLocalMaybe).thenReturn(maybe)
    }

    @Test
    @Ignore
    fun onLaunch_logAnalytics() {
        verify(activity.analytics).log(AmplitudeEvents.APP_LAUNCHED)
    }

    @Test
    @Ignore
    fun whenUserIsCreatedButOnboardingNotCompleted_thenShowOnboarding() {
        `when`(activity.userDataProvider.userNextStep).thenReturn(Authorization.NextStep.SIGN_UP)
        activityController.resume()
        maybe.test().assertComplete()

        activity.assertLaunched(OnboardingActivity::class.java)
    }

    @SuppressLint("Registered")
    class LaunchActivityStub : LaunchActivity() {
        @Mock lateinit var mockUserDataProvider: UserDataProvider
        @Mock lateinit var mockOnboardingDataProvider: OnboardingDataProvider
        @Mock lateinit var mockFeatureDataProvider: FeatureDataProvider

        override fun performInjection() {
            MockitoAnnotations.initMocks(this)
            userDataProvider = mockUserDataProvider
            featureDataProvider = mockFeatureDataProvider
            onboardingDataProvider = mockOnboardingDataProvider
            analytics = mock(Analytics::class.java)
        }
    }
}