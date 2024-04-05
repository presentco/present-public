package co.present.present.feature.invite

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import co.present.present.R
import co.present.present.extensions.applyCompletableSchedulers
import co.present.present.extensions.applyFlowableSchedulers
import co.present.present.extensions.snackbar
import co.present.present.extensions.string
import co.present.present.feature.welcome.FacebookPromptFragment
import co.present.present.location.ContactPermissions
import co.present.present.model.Circle
import co.present.present.model.CurrentUser
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.OnItemClickListener
import com.xwray.groupie.ViewHolder
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_add_friends_to_circle.*
import javax.inject.Inject

/**
 * A screen that offers to text your phone contacts a link to join the app via SMS.
 */
class AddToCircleActivity : BaseActivity(), OnItemClickListener, ContactsPermissionPlaceholderFragment.ContactsPermissionListener {

    private val TAG = javaClass.simpleName
    val circleId: String by lazy { intent.getStringExtra(Circle.ARG_CIRCLE) }
    private val adapter = GroupAdapter<ViewHolder>().apply { setOnItemClickListener(this@AddToCircleActivity) }
    @Inject
    lateinit var contactPermission: ContactPermissions

    override fun performInjection() {
        activityComponent.inject(this)
    }

    private lateinit var viewModel: AddToCircleViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friends_to_circle)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(AddToCircleViewModel::class.java)

        setSupportActionBar(toolbar as Toolbar)
        supportActionBar?.apply {
            setTitle(R.string.add_friends_to_circle)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close)
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
                                    viewPager.offscreenPageLimit = 3
                                    viewPager.adapter = ViewPagerAdapter(it, supportFragmentManager)
                                }
                            }
                        }
                ).addTo(disposable)
    }

    inner class ViewPagerAdapter(val currentUser: CurrentUser, fm: FragmentManager): FragmentStatePagerAdapter(fm) {
        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> AddToCircleFriendsFragment.newInstance(circleId)
                1 -> if (contactPermission.isGranted(this@AddToCircleActivity)) AddToCircleContactsFragment.newInstance(circleId) else ContactsPermissionPlaceholderFragment()
                else -> if (currentUser.facebookLinked) AddToCircleFacebookFriendsFragment.newInstance(circleId) else FacebookPromptFragment()
            }
        }

        override fun getCount() = 3

        override fun getPageTitle(position: Int): CharSequence? {
            return string(when (position) {
                0 -> R.string.friends
                1 -> R.string.contacts
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.add_to_circle, menu)
        disposable += viewModel.submitButtonEnabled().compose(applyFlowableSchedulers())
                .subscribe { enabled ->
                    menu.findItem(R.id.add).isEnabled = enabled
                }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> addInviteesToCircle()
            android.R.id.home -> finish()
        }
        return true
    }

    private fun addInviteesToCircle() {
        disposable += viewModel.addInviteesToCircle(circleId).compose(applyCompletableSchedulers())
                .subscribeBy(
                        onError = {
                            snackbar(R.string.network_error)
                        },
                        onComplete = {
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                )
    }

    companion object {

        fun newIntent(context: Context, circleId: String): Intent {
            val intent = Intent(context, AddToCircleActivity::class.java)
            intent.putExtra(Circle.ARG_CIRCLE, circleId)
            return intent
        }

    }
}