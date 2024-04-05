package co.present.present.location

import android.os.Bundle
import co.present.present.BaseActivity
import co.present.present.LaunchActivity
import co.present.present.R
import co.present.present.extensions.start
import co.present.present.extensions.transaction

class LocationPermissionActivity : BaseActivity() {
    override fun performInjection() {
        activityComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empty)

        supportFragmentManager.transaction {
            add(R.id.content, LocationFragment())
        }
    }
}

class LocationFragment: BaseLocationPermissionFragment() {
    override fun onLocationSuccess() {
        activity?.start<LaunchActivity>()
        activity?.finish()
    }

    override fun logAllowLocationAccessClicked() {
        // we don't want to do the original logging, which is part of sign up
    }
}
