package co.present.present.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

/**
 * Extracted interface for runtime Android permissions for testing.
 *
 *
 * Copied from http://dannyroa.com/2016/04/11/android-making-espresso-tests-work-with-runtime-permissions/
 */
open class PermissionsImpl(val permission: String) : Permissions {
    override fun isGranted(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun shouldShowRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    override fun requestPermission(activity: Activity, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
    }
}

open class LocationPermissions: PermissionsImpl(Manifest.permission.ACCESS_FINE_LOCATION)

object ContactPermissions: PermissionsImpl(Manifest.permission.READ_CONTACTS)
