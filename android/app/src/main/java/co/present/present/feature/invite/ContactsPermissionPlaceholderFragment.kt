package co.present.present.feature.invite

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProviders
import co.present.present.R
import co.present.present.extensions.dialog
import co.present.present.extensions.showApplicationSettings
import co.present.present.feature.welcome.ContactsPromptFragment
import co.present.present.location.ContactPermissions
import co.present.present.location.isPermissionGranted
import com.xwray.groupie.OnItemClickListener
import javax.inject.Inject


class ContactsPermissionPlaceholderFragment : ContactsPromptFragment(), OnItemClickListener {


    private val TAG = javaClass.simpleName

    @Inject lateinit var contactPermission: ContactPermissions
    private lateinit var viewModel: AddToCircleViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityComponent.inject(this)
    }

    override fun onResume() {
        super.onResume()
        if (contactPermission.isGranted(baseActivity)) {
            Log.d(TAG, "Contact permission granted")
            onContactsPermissionGranted()
        }
    }

    override fun onConnectContactsClicked() {
        contactPermission.requestPermission(baseActivity, REQUEST_READ_CONTACTS)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(AddToCircleViewModel::class.java)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (resultCode == Activity.RESULT_OK) {
                // Try again to start the chain of events requiring contacts.
                onContactsPermissionGranted()
            }
        }
    }

    interface ContactsPermissionListener {
        fun onContactsPermissionGranted()
    }

    private fun onContactsPermissionGranted() {
        (baseActivity as ContactsPermissionListener).onContactsPermissionGranted()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (!grantResults.isPermissionGranted() && requestCode == REQUEST_READ_CONTACTS) {
            when {
                contactPermission.shouldShowRationale(baseActivity) -> showRationaleDialog()
                else -> showRedirectToSettingsDialog()
            }
        }
    }

    private fun showRationaleDialog() {
        dialog(R.string.read_contacts_rationale_text,
                title = R.string.read_contacts_title,
                positiveButtonText = R.string.ok) {
            contactPermission.requestPermission(baseActivity, REQUEST_READ_CONTACTS)
        }
    }

    private fun showRedirectToSettingsDialog() {
        dialog(R.string.read_contacts_settings_instructions,
                title = R.string.read_contacts_title,
                positiveButtonText = R.string.app_settings) {
            baseActivity.showApplicationSettings()
        }
    }

    companion object {

        const val REQUEST_READ_CONTACTS = 1234

    }
}