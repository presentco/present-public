package co.present.present.feature.onboarding.step

import android.annotation.SuppressLint
import androidx.constraintlayout.widget.ConstraintLayout
import android.widget.TextView
import co.present.present.BuildConfig
import co.present.present.R
import co.present.present.TestPresentApplication
import co.present.present.analytics.AmplitudeEvents
import co.present.present.analytics.Analytics
import co.present.present.config.FeatureDataProvider
import co.present.present.feature.onboarding.OnboardingDataProvider
import co.present.present.support.ViewLocator.getView
import co.present.present.user.UserDataProvider
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.squareup.otto.Bus
import dagger.Lazy
import io.reactivex.Observable
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import present.proto.AuthorizationResponse


@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, application = TestPresentApplication::class)
@Ignore
class FacebookLinkActivityTest {
    private lateinit var activity: FacebookLinkActivity
    private lateinit var loginButton: ConstraintLayout
    private lateinit var tosText: TextView
    @Captor private lateinit var facebookCallbackCaptor: ArgumentCaptor<FacebookCallback<LoginResult>>
    @Mock lateinit var loginResult: LoginResult
    @Mock lateinit var accessToken: AccessToken
    lateinit var controller: ActivityController<FacebookLinkActivityStub>

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        controller = Robolectric.buildActivity(FacebookLinkActivityStub::class.java).create()
        activity = controller.get()

        loginButton = getView(activity, R.id.facebookButton)
        tosText = getView(activity, R.id.termsOfService)
    }

    @Test
    fun whenActivityIsLaunched_thenOnboardingStartIsLoggedInAnalytics() {
        verify(activity.analytics).log(AmplitudeEvents.FACEBOOK_CONNECT_VIEW)
    }

    @Test
    fun facebookLoginStarts() {
        verify(activity.loginManager).logInWithReadPermissions(activity, arrayListOf("public_profile", "email", "user_friends"))
    }

    fun getFacebookCallback(): FacebookCallback<LoginResult> {
        `when`(loginResult.accessToken).thenReturn(accessToken)
        `when`(accessToken.token).thenReturn("token")
        `when`(activity.facebookIdProvider.get()).thenReturn("id")
        `when`(activity.onboardingDataProvider.linkFacebook(anyString(), anyString()))
                .thenReturn(Observable.just(mock(AuthorizationResponse::class.java)))

        verify(activity.loginManager).registerCallback(any(CallbackManager::class.java), facebookCallbackCaptor.capture())
        return facebookCallbackCaptor.value
    }

    @Test
    fun whenLoginSucceeds_andFacebookAppInstalled_thenFacebookLinkingStarts() {
        getFacebookCallback().onSuccess(loginResult)

        verify(activity.onboardingDataProvider).linkFacebook("token", "id")
    }

    @Test
    fun whenFacebookLoginSucceeds_thenLogFacebookAccessSuccess() {
        getFacebookCallback().onSuccess(loginResult)

        verify(activity.analytics).log(AmplitudeEvents.FACEBOOK_CONNECT_SUCCESS)
    }

    @Test
    fun whenFacebookLoginCancelled_thenLogFacebookAccessFailure() {
        getFacebookCallback().onCancel()

        verify(activity.analytics).log(AmplitudeEvents.FACEBOOK_CONNECT_ERROR)
    }

    @Test
    fun verifyBusIsRegistered() {
        verify(activity.bus).register(activity)
    }

    @Test
    fun verifyBusIsUnregistered() {
        controller.destroy()
        verify(activity.bus).unregister(activity)
    }

    @SuppressLint("Registered")
    class FacebookLinkActivityStub : FacebookLinkActivity() {

        @Mock
        override lateinit var facebookIdProvider: Lazy<String>

        override fun performInjection() {
            MockitoAnnotations.initMocks(this)
            loginManager = mock(LoginManager::class.java)
            callbackManager = mock(CallbackManager::class.java)
            bus = mock(Bus::class.java)
            onboardingDataProvider = mock(OnboardingDataProvider::class.java)
            featureDataProvider = mock(FeatureDataProvider::class.java)
            userDataProvider = mock(UserDataProvider::class.java)
            analytics = mock(Analytics::class.java)
        }
    }
}
