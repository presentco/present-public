package co.present.present.feature.onboarding

import co.present.present.location.BaseLocationPermissionFragment

/**
 * Fragment to request runtime permissions
 */
open class PlaceholderLocationPermissionFragment : BaseLocationPermissionFragment() {

    override fun onLocationSuccess() {
        (parentFragment!! as LocationListener).onLocationSuccess()
    }

    override fun performInjection() {
        activityComponent.inject(this)
    }

    interface LocationListener {
        fun onLocationSuccess()
    }
}
