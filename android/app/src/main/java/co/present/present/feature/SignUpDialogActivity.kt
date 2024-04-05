package co.present.present.feature

import android.os.Bundle
import android.view.Window
import co.present.present.BaseActivity
import co.present.present.R
import co.present.present.analytics.AmplitudeEvents
import co.present.present.analytics.Analytics
import co.present.present.extensions.start
import co.present.present.feature.onboarding.PhoneLoginActivity
import kotlinx.android.synthetic.main.activity_sign_up.*
import javax.inject.Inject

class SignUpDialogActivity: BaseActivity() {

    // Can't inflate the debug drawer in combination with the dialog theme.
    // This is sort of a hack -- if we end up with more dialogs, make this more formalized
    override val showDebugDrawer = false

    override fun performInjection() {
        activityComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_sign_up)
        analytics.log(AmplitudeEvents.JOIN_PRESENT_VIEW)

        verifyPhoneButton.setOnClickListener {
            analytics.log(AmplitudeEvents.JOIN_PRESENT_TAP_GET_STARTED)
            start<PhoneLoginActivity>()
            finish()
        }

        closeButton.setOnClickListener {
            analytics.log(AmplitudeEvents.JOIN_PRESENT_CANCEL)
            finish()
        }
    }
}