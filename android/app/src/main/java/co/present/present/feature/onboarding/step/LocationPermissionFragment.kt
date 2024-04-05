package co.present.present.feature.onboarding.step

import co.present.present.feature.onboarding.events.AdvanceOnboardingEvent
import co.present.present.location.BaseLocationPermissionFragment
import com.squareup.otto.Bus
import javax.inject.Inject

/**
 * Fragment to request runtime permissions
 */
open class LocationPermissionFragment : BaseLocationPermissionFragment() {
    @Inject lateinit open var bus: Bus

    override fun onLocationSuccess() {
        bus.post(AdvanceOnboardingEvent())
    }

    override fun performInjection() {
        activityComponent.inject(this)
    }
}
