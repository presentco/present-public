package co.present.present.feature

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.Menu
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.ViewModelProviders
import co.present.present.BaseActivity
import co.present.present.R
import co.present.present.Synchronize
import co.present.present.SynchronizeImpl
import co.present.present.extensions.applyCompletableSchedulers
import co.present.present.extensions.slideOverFromBottom
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.feature.create.EditCircleActivity
import co.present.present.feature.discovery.FeedFragment
import co.present.present.feature.discovery.FeedViewModel
import co.present.present.feature.profile.UserProfileFragment
import co.present.present.feature.welcome.CreateCirclePlaceholderFragment
import co.present.present.feature.welcome.ProfilePlaceholderFragment
import co.present.present.service.rpc.putToken
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

class MainActivity : BaseActivity(), Synchronize by SynchronizeImpl(), BottomNav.BottomNavListener {
    private val TAG = javaClass.simpleName

    private lateinit var bottomNav: BottomNav
    @Inject lateinit var getCurrentUser: GetCurrentUser
    private val feedViewModel: FeedViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(FeedViewModel::class.java)
    }

    override fun performInjection() {
        activityComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = BottomNav(this, this)
        val citiesViewModel = ViewModelProviders.of(this, viewModelFactory).get(CitiesViewModel::class.java)
        citiesViewModel.getFromNetworkAndSaveAsync()

        checkNotificationToken()

        onNewIntent(intent)
    }

    private fun checkNotificationToken() {
        if (!userDataProvider.tokenUploaded && userDataProvider.firebaseToken != null) {
            userService.putToken(userDataProvider.firebaseToken)
                    .compose(applyCompletableSchedulers())
                    .doOnComplete { userDataProvider.tokenUploaded = true }
                    .subscribeBy(
                            onError = { Log.e(TAG, "Couldn't upload notification token", it) },
                            onComplete = { Log.d(TAG, "Successfully uploaded new device notification token") }
                    )
        }
    }

    override fun onNewIntent(intent: Intent) {
        intent.data?.let { uri ->
            launchDeepLink(uri)
        }
    }

    override fun onResume() {
        super.onResume()
        disposable += synchronize(this, userDataProvider, getCurrentUser)
    }

    private val fragments = SparseArray<Fragment>()

    override fun onCreateFragments(state: BottomNavState) {
        Log.d(TAG, "onCreateFragments: $state")

        viewPager.offscreenPageLimit = 3

            viewPager.adapter = object: FragmentStatePagerAdapter(supportFragmentManager) {
                override fun getItem(position: Int): Fragment {
                    val fragment =  when (position) {
                        0 -> FeedFragment()
                        1 -> CreateCirclePlaceholderFragment()
                        else -> {
                            if (state is BottomNavState.LoggedIn) UserProfileFragment.newInstance(state.currentUser.id)
                            else ProfilePlaceholderFragment()
                        }
                    }
                    fragments.put(position, fragment)
                    return fragment
                }

                override fun getCount() = 3

//                override fun getItemPosition(`object`: Any): Int {
//                    return PagerAdapter.POSITION_NONE
//                }
            }
        viewPager.postDelayed(500) {
            viewPager.setCurrentItem(bottomNav.selectedTabIndex, true)
            viewPager.adapter?.notifyDataSetChanged()
        }
    }


    /**
     * Returns whether or not the tab should be shown as selected.
     */
    override fun onBottomNavTabSelected(index: Int, state: BottomNavState): Boolean {
        // When logged in, the middle tab has special handling: launch Create Circle as full screen
        // and don't select the tab
        if (state is BottomNavState.LoggedIn && index == 1) {
            slideOverFromBottom<EditCircleActivity>()
            return false
        }

        if (bottomNav.selectedTabIndex == index) {
            val selectedFragment = fragments[index]
            if (selectedFragment is Resettable) selectedFragment.resetToInitialState()
            return true
        } else {
            viewPager.setCurrentItem(index, false)
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        return true
    }

    private fun getCurrentFragment(): Fragment {
        return fragments[bottomNav.selectedTabIndex]
    }

    override fun onBackPressed() {
        val currentFragment = getCurrentFragment()
        if (currentFragment is Stack && currentFragment.canGoBack()) {
            currentFragment.goBack()
        } else if (bottomNav.selectedTabIndex > 0) {
            bottomNav.goBack()
        } else {
            finish()
        }
    }

    interface Stack {
        fun canGoBack(): Boolean

        fun goBack()
    }

    interface Scrollable {
        fun scrollToTop()
    }

    interface Resettable {
        fun resetToInitialState()
    }
}