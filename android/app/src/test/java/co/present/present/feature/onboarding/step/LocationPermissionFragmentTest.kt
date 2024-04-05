package co.present.present.feature.onboarding.step

import android.content.pm.PackageManager
import co.present.present.BuildConfig
import co.present.present.analytics.AmplitudeEvents
import co.present.present.analytics.Analytics
import co.present.present.location.LocationDataProvider
import co.present.present.location.LocationPermissions
import co.present.present.location.Permissions
import co.present.present.location.permissionsRequestCodeAccessFineLocation
import com.squareup.otto.Bus
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.support.v4.SupportFragmentTestUtil.startVisibleFragment

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class LocationPermissionFragmentTest {
    private lateinit var fragment: LocationPermissionFragmentStub

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        fragment = LocationPermissionFragmentStub()
        startVisibleFragment(fragment)
    }

    @Test
    @Ignore
    fun whenLocationPermissionAccepted_LogAnalytics() {
        fragment.onRequestPermissionsResult(
                permissionsRequestCodeAccessFineLocation,
                emptyArray(),
                intArrayOf(PackageManager.PERMISSION_GRANTED))
        verify(fragment.analytics).log(AmplitudeEvents.SIGNUP_VIEW_ALLOW_LOCATION)
    }

    @Test
    @Ignore
    fun whenLocationPermissionRejected_LogAnalytics() {
        fragment.onRequestPermissionsResult(
                permissionsRequestCodeAccessFineLocation,
                emptyArray(),
                intArrayOf())
        verify(fragment.analytics).log(AmplitudeEvents.SIGNUP_LOCATION_DENIED)
    }

    @Test
    @Ignore
    fun whenLocationPermissionEnabledManuallyInSettings_LogAnalytics() {
        `when`(fragment.locationPermissions.isGranted(fragment.requireContext())).thenReturn(true)
        fragment.onResume()
        verify(fragment.analytics).log(AmplitudeEvents.SIGNUP_LOCATION_ALLOWED)
    }

    class LocationPermissionFragmentStub : LocationPermissionFragment() {

        override fun performInjection() {
            bus = Mockito.mock(Bus::class.java)
            locationPermissions = Mockito.mock(LocationPermissions::class.java)
            locationDataProvider = Mockito.mock(LocationDataProvider::class.java)
            analytics = Mockito.mock(Analytics::class.java)
        }
    }
}