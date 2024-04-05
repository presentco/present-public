package co.present.present.feature.invite

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.PagerAdapter
import co.present.present.BaseActivity
import co.present.present.LaunchActivity
import co.present.present.R
import co.present.present.extensions.applyFlowableSchedulers
import co.present.present.extensions.start
import co.present.present.extensions.string
import co.present.present.feature.welcome.FacebookPromptFragment
import co.present.present.location.ContactPermissions
import co.present.present.model.CurrentUser
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.OnItemClickListener
import com.xwray.groupie.ViewHolder
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_add_friends.*
import javax.inject.Inject

class OnboardingAddFriendsActivity: AddFriendsActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.apply {
            setHomeButtonEnabled(false)
            setDisplayHomeAsUpEnabled(false)
        }
    }

    override fun onContactsPermissionGranted() {
        super.onContactsPermissionGranted()
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.add_friends, menu)
        viewModel.currentUser.compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = {},
                        onNext = {
                            if (it.facebookLinked || contactPermission.isGranted(this)) {
                                menu?.findItem(R.id.skip)?.title = string(R.string.done)
                            }
                        }
                ).addTo(disposable)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        start<LaunchActivity>()
        finish()
        return true
    }
}

open class AddFriendsActivity : BaseActivity(), OnItemClickListener, ContactsPermissionPlaceholderFragment.ContactsPermissionListener {

    private val TAG = javaClass.simpleName
    private val adapter = GroupAdapter<ViewHolder>().apply { setOnItemClickListener(this@AddFriendsActivity) }
    @Inject lateinit var contactPermission: ContactPermissions

    override fun performInjection() {
        activityComponent.inject(this)
    }

    protected lateinit var viewModel: AddFriendsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friends)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(AddFriendsViewModel::class.java)

        setSupportActionBar(toolbar as Toolbar)
        supportActionBar?.apply {
            setTitle(R.string.add_friends)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        disposable += viewModel.getItems().compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = { e ->
                            Log.e(TAG, "error in viewmodel", e)
                        },
                        onNext = { items ->
                            adapter.update(items)
                        }
                )

        viewModel.currentUser.compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = {},
                        onNext = {
                            val adapter = viewPager.adapter as ViewPagerAdapter?
                            if (adapter == null || (!adapter.currentUser.facebookLinked && it.facebookLinked)) {
                                viewPager.post {
                                    tabs.setupWithViewPager(viewPager)
                                    viewPager.offscreenPageLimit = 2
                                    viewPager.adapter = ViewPagerAdapter(it, supportFragmentManager)
                                }
                            }
                        }
                ).addTo(disposable)
    }

    inner class ViewPagerAdapter(val currentUser: CurrentUser, fm: FragmentManager): FragmentStatePagerAdapter(fm) {
        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> if (contactPermission.isGranted(this@AddFriendsActivity)) AddFriendsContactsFragment() else ContactsPermissionPlaceholderFragment()
                else -> if (currentUser.facebookLinked) AddFriendsFacebookFragment() else FacebookPromptFragment()
            }
        }

        override fun getCount() = 2

        override fun getPageTitle(position: Int): CharSequence? {
            return string(when (position) {
                0 -> R.string.contacts
                else -> R.string.facebook
            })
        }

        override fun getItemPosition(`object`: Any): Int {
            return PagerAdapter.POSITION_NONE
        }
    }

    override fun onContactsPermissionGranted() {
        // Important to post this, to make sure it goes to the end of the
        // fragment transaction queue.
        // https://stackoverflow.com/questions/38722325/fragmentmanager-is-already-executing-transactions-when-is-it-safe-to-initialise
        viewPager.post {
            viewPager.adapter?.notifyDataSetChanged()
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }
}