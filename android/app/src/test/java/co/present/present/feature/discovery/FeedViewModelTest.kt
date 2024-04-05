package co.present.present.feature.discovery

import android.app.Application
import android.location.Location
import co.present.present.analytics.Analytics
import co.present.present.config.FeatureDataProvider
import co.present.present.db.CircleDao
import co.present.present.feature.BottomNavState
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.feature.detail.info.JoinCircle
import co.present.present.location.LocationDataProvider
import co.present.present.support.DataHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import present.proto.Gender

class FeedViewModelTest {

    lateinit var testObject: FeedViewModel
    @Mock lateinit var circleDao: CircleDao
    @Mock lateinit var analytics: Analytics
    @Mock lateinit var application: Application
    @Mock lateinit var locationDataProvider: LocationDataProvider
    @Mock lateinit var featureDataProvider: FeatureDataProvider
    @Mock lateinit var joinCircle: JoinCircle
    @Mock lateinit var getCurrentUser: GetCurrentUser
    @Mock lateinit var searchable: Searchable
    @Mock lateinit var refreshCircles: RefreshCircles


    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        testObject = FeedViewModel(circleDao, locationDataProvider, featureDataProvider, joinCircle,
                getCurrentUser, searchable, refreshCircles, analytics, application)
    }

    @Test
    fun whenUserIsLoggedOutAndHidWomenOnlyTab_shouldHideWomenOnlyContentIsTrue() {
        val state = BottomNavState.LoggedOut(hidWomenOnlyTab = true)
        assertTrue(testObject.shouldHideWomenOnlyContent(state))
    }

    @Test
    fun whenUserIsLoggedOutAndDidntHideWomenOnlyTab_shouldHideWomenOnlyContentIsFalse() {
        val state = BottomNavState.LoggedOut(hidWomenOnlyTab = false)
        assertFalse(testObject.shouldHideWomenOnlyContent(state))
    }

    @Test
    fun whenUserIsManOnFacebook_shouldHideWomenOnlyContentIsTrue() {
        val state = BottomNavState.LoggedIn(
                hidWomenOnlyTab = false,
                currentUser = manOnFacebook(),
                location = Location("")
        )
        assertTrue(testObject.shouldHideWomenOnlyContent(state))
    }

    @Test
    fun whenUserIsWomanOnFacebook_shouldHideWomenOnlyContentIsFalse() {
        val state = BottomNavState.LoggedIn(
                hidWomenOnlyTab = false,
                currentUser = womanOnFacebook(),
                location = Location("")
        )
        assertFalse(testObject.shouldHideWomenOnlyContent(state))
    }


    private fun womanOnFacebook() = DataHelper.getCurrentUser().copy(gender = Gender.WOMAN.value)
    private fun manOnFacebook() = DataHelper.getCurrentUser().copy(gender = Gender.MAN.value)


}