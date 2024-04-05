package co.present.present.extensions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import co.present.present.R


fun Context.isGranted(permission: String): Boolean {
    return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

fun Activity.shouldShowRationale(permission: String): Boolean {
    return ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
}

fun Activity.request(permission: String, requestCode: Int) {
    ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
}

object WriteStoragePermissionHandler: PermissionHandler(
        permission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
        requestCode = 1,
        dialogTitle = R.string.write_storage_title,
        dialogRationale = R.string.write_storage_rationale_text,
        dialogSettingsPrompt = R.string.write_storage_settings_text)




open class PermissionHandler(val permission: String,
                        val requestCode: Int,
                        val dialogTitle: Int,
                        val dialogRationale: Int,
                        val dialogSettingsPrompt: Int) {

    fun request(fragment: Fragment, actionWithPermission: () -> Unit) {
        fragment.activity?.apply {
            if (isGranted(permission)) {
                actionWithPermission()
            } else {
                fragment.requestPermissions(arrayOf(permission), requestCode)
            }
        }
    }

    fun onRequestPermissionsResult(activity: AppCompatActivity?, requestCode: Int, permissions: Array<out String>, grantResults: IntArray, actionWithPermission: () -> Unit) {
        activity?.apply {
            val permissionIndex = permissions.indexOf(permission)
            val grantResult = if (grantResults.size > permissionIndex) grantResults[permissionIndex] else -1

            if (grantResult == PackageManager.PERMISSION_GRANTED && requestCode == this@PermissionHandler.requestCode) {
                actionWithPermission()
            } else {
                when {
                    shouldShowRationale(permission) -> showRationaleDialog(activity, requestCode)
                    else -> showRedirectToSettingsDialog(activity)
                }
            }
        }
    }

    private fun showRationaleDialog(activity: AppCompatActivity?, requestCode: Int) {
        activity?.apply {
            dialog(dialogRationale,
                    title = dialogTitle,
                    negativeButtonText = R.string.not_now,
                    positiveButtonText = R.string.ok) {
                request(permission, requestCode)
            }
        }
    }

    private fun showRedirectToSettingsDialog(activity: AppCompatActivity?) {
        activity?.apply {
            dialog(dialogSettingsPrompt,
                    title = dialogTitle,
                    negativeButtonText = R.string.not_now,
                    positiveButtonText = R.string.app_settings) {
                showApplicationSettings()
            }
        }
    }

}
