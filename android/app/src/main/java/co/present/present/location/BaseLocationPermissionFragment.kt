package co.present.present.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import co.present.present.R
import co.present.present.analytics.AmplitudeEvents
import co.present.present.extensions.*
import co.present.present.feature.welcome.LocationPromptFragment
import com.google.android.gms.common.api.ResolvableApiException
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.reactivex.rxkotlin.subscribeBy
import javax.inject.Inject


/**
 * Fragment to request runtime permissions
 */
open class BaseLocationPermissionFragment : LocationPromptFragment() {
    private val TAG = javaClass.simpleName
    @Inject lateinit var locationPermissions: LocationPermissions
    @Inject lateinit var locationDataProvider: LocationDataProvider

    val adapter = GroupAdapter<ViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        performInjection()
        analytics.log(AmplitudeEvents.SIGNUP_VIEW_ALLOW_LOCATION)
    }

    override fun onAllowLocationAccessClicked() {
        requestLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        if (locationPermissions.isGranted(baseActivity)) {
            Log.d(TAG, "Location already granted, trying to get location")
            onLocationPermissionGranted()
        }
    }

    protected open fun performInjection() {
        activityComponent.inject(this)
    }

    @SuppressLint("MissingPermission") // Permission check already executed
    private fun onLocationPermissionGranted() {
        analytics.log(AmplitudeEvents.SIGNUP_LOCATION_ALLOWED)
        locationDataProvider.getLocation(context!!).compose(applySingleSchedulers())
                .subscribeBy(
                        onError = { e ->
                            when (e) {
                                is ResolvableApiException -> tellUserToTurnOnDeviceLocation(e)
                                is SecurityException -> showLocationPermissionMissingSnackbar()
                                else -> snackbar(R.string.couldnt_get_location)
                            }
                        },
                        onSuccess = {
                            onLocationSuccess()
                        }
                )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                // Try again to start the chain of events requiring location.
                onLocationPermissionGranted()
            } else {
                showTurnOnDeviceLocationDialog()
            }
        }
    }

    /**
     * This happens after we receive a failure from checking that the device location setting is enabled.
     */
    private fun tellUserToTurnOnDeviceLocation(e: ResolvableApiException) {
        try {
            // Launches a system dialog with an explanation, which can actually change the setting
            // on the spot for us if the user clicks "OK".
            e.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS)
        } catch (sendEx: IntentSender.SendIntentException) {
            // I don't know why this error would happen (some system thing)?
            // but in this case, deliver our own explanation to the user and send them to Settings
            showTurnOnDeviceLocationDialog()
        }
    }

    private fun showTurnOnDeviceLocationDialog() {
        dialog(title = R.string.onboarding_location_prompt,
                message = R.string.location_settings_instructions,
                positiveButtonText = R.string.go_to_settings) {
            activity?.showLocationSettings()
        }
    }

    open fun onLocationSuccess() {}

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (!grantResults.isPermissionGranted() && requestCode == permissionsRequestCodeAccessFineLocation) {
            when {
                locationPermissions.shouldShowRationale(baseActivity) -> {
                    showRationaleDialog()
                    analytics.log(AmplitudeEvents.SIGNUP_LOCATION_DENIED)
                }
                else -> {
                    showRedirectToSettingsDialog()
                    showLocationPermissionMissingSnackbar()
                    analytics.log(AmplitudeEvents.SIGNUP_LOCATION_DENIED)
                }
            }
        }
    }

    private fun showLocationPermissionMissingSnackbar() {
        snackbar(R.string.location_snackbar_prompt,
                actionStringRes = R.string.ok) {
            showRedirectToSettingsDialog()
        }
    }

    var shouldRequestLocationPermission = false

    private fun showRationaleDialog() {
//        dialog(R.string.location_rationale,
//                title = R.string.onboarding_location_prompt,
//                positiveButtonText = R.string.ok) {
//
//            requestLocationPermission()
//        }
    }

    private fun showRedirectToSettingsDialog() {
        dialog(R.string.location_permission_settings_instructions,
                title = R.string.onboarding_location_prompt,
                positiveButtonText = R.string.app_settings) {
            activity?.showApplicationSettings()
            analytics.log(AmplitudeEvents.SIGNUP_USER_PROMPTED_TO_ENABLE_LOCATION_IN_SETTINGS)
        }
    }

    private fun requestLocationPermission() {
        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                permissionsRequestCodeAccessFineLocation)
        analytics.log(AmplitudeEvents.SIGNUP_USER_PROMPTED_FOR_LOCATION_PERMISSION)
    }

    companion object {
        val REQUEST_CHECK_SETTINGS = 123
    }

}

fun IntArray.isPermissionGranted(): Boolean {
    return isNotEmpty() && this[0] == PackageManager.PERMISSION_GRANTED
}
