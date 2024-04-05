package co.present.present.feature.onboarding.step

import android.net.Uri
import androidx.annotation.StringRes
import co.present.present.BuildConfig
import co.present.present.R
import co.present.present.analytics.AmplitudeEvents
import co.present.present.analytics.AmplitudeKeys
import co.present.present.analytics.AmplitudeValues
import co.present.present.analytics.Analytics
import co.present.present.config.FeatureDataProvider
import co.present.present.feature.onboarding.WaitlistActivity
import co.present.present.support.Assert.assertViewIsVisible
import co.present.present.user.UserDataProvider
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import junit.framework.Assert.assertEquals
import kotlinx.android.synthetic.main.activity_waitlist.*
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication
import present.proto.Authorization
import present.proto.BlockScreen
import present.proto.SynchronizeResponse
import present.proto.UserProfile


@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
@Ignore
class WaitlistActivityTest {
    private lateinit var activity: WaitlistActivityStub

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        activity = Robolectric.setupActivity(WaitlistActivityStub::class.java)
    }

    @Test
    fun reportWaitlistToAnalytics() {
        verify(activity.analytics).log(AmplitudeEvents.SIGNUP_VIEW_WAIT_FOR_APPROVAL)
    }

    @Test
    fun shouldHaveTitle() {
        assertViewIsVisible(activity.description)

        assertThat(activity.description.text, equalTo<CharSequence>("blocked, mofo!!"))
    }

    @Test
    fun facebookButtonShouldBeVisible() {
        assertViewIsVisible(activity.facebookButton)
    }

    @Test
    fun whenFacebookButtonIsClicked_thenFacebookUrlLaunches() {
        activity.facebookButton.performClick()

        val expectedUrl = getSocialUri(R.string.social_facebook)
        val actual = ShadowApplication.getInstance().nextStartedActivity
        assertEquals(expectedUrl, actual.data)
    }

    @Test
    fun whenFacebookButtonIsClicked_thenReportToAnalytics() {
        activity.facebookButton.performClick()
        verify(activity.analytics).log(AmplitudeEvents.SIGNUP_FOLLOW_SOCIAL_LINK,
                Analytics.SingleEventProperty(AmplitudeKeys.SOCIAL_MEDIA, AmplitudeValues.FACEBOOK))
    }

    @Test
    fun instagramButtonShouldBeVisible() {
        assertViewIsVisible(activity.instagramButton)
    }

    @Test
    fun whenInstagramButtonIsClicked_thenInstagramUrlLaunches() {
        activity.instagramButton.performClick()

        val expectedUrl = getSocialUri(R.string.social_instagram)
        val actual = ShadowApplication.getInstance().nextStartedActivity
        assertEquals(expectedUrl, actual.data)
    }

    @Test
    fun whenInstagramButtonIsClicked_thenReportToAnalytics() {
        activity.instagramButton.performClick()
        verify(activity.analytics).log(AmplitudeEvents.SIGNUP_FOLLOW_SOCIAL_LINK,
                Analytics.SingleEventProperty(AmplitudeKeys.SOCIAL_MEDIA, AmplitudeValues.INSTAGRAM))    }

    @Test
    fun twitterButtonShouldBeVisible() {
        assertViewIsVisible(activity.twitterButton)
    }

    @Test
    fun whenTwitterButtonIsClicked_thenTwitterUrlLaunches() {
        activity.twitterButton.performClick()

        val expectedUrl = getSocialUri(R.string.social_twitter)
        val actual = ShadowApplication.getInstance().nextStartedActivity
        assertEquals(expectedUrl, actual.data)
    }

    @Test
    fun whenTwitterButtonIsClicked_thenReportToAnalytics() {
        activity.twitterButton.performClick()
        verify(activity.analytics).log(AmplitudeEvents.SIGNUP_FOLLOW_SOCIAL_LINK,
                Analytics.SingleEventProperty(AmplitudeKeys.SOCIAL_MEDIA, AmplitudeValues.TWITTER))
    }

    fun getSocialUri(@StringRes urlResId: Int): Uri {
        val url = ShadowApplication.getInstance().applicationContext.resources.getString(urlResId)
        return Uri.parse(url)
    }

    class WaitlistActivityStub : WaitlistActivity() {

        override fun performInjection() {
            analytics = mock(Analytics::class.java)
            userDataProvider = mock(UserDataProvider::class.java)
            featureDataProvider = mock(FeatureDataProvider::class.java)
            val userProfile = mock(UserProfile::class.java)
            val syncResponse = SynchronizeResponse(Authorization(Authorization.NextStep.BLOCK, BlockScreen("blocked, mofo!!")), userProfile, listOf())
            whenever(userDataProvider.synchronize()) doReturn(Single.just(syncResponse))
        }
    }
}