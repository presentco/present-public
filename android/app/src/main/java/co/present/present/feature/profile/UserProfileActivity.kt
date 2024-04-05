package co.present.present.feature.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import co.present.present.BaseActivity
import co.present.present.R
import co.present.present.Synchronize
import co.present.present.SynchronizeImpl
import co.present.present.analytics.AmplitudeEvents
import co.present.present.extensions.transaction
import co.present.present.model.User

open class UserProfileActivity : BaseActivity(), Synchronize by SynchronizeImpl() {
    private val TAG = javaClass.simpleName

    val userId: String by lazy { intent.getStringExtra(User.USER) }
    open val contentView = R.layout.activity_empty

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView)
        analytics.log(AmplitudeEvents.USER_VIEW_PROFILE)

        val tag = "profile"
        if (supportFragmentManager.findFragmentByTag(tag) == null) {
            supportFragmentManager.transaction {
                add(R.id.content, UserProfileFragment.newInstance(userId), tag)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

    override fun performInjection() {
        activityComponent.inject(this)
    }

    companion object {

        fun newIntent(context: Context, userId: String): Intent {
            val intent = Intent(context, UserProfileActivity::class.java)
            intent.putExtra(User.USER, userId)
            return intent
        }
    }
}
