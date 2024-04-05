package co.present.present.feature.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.ViewModelProviders
import co.present.present.BaseFragment
import co.present.present.R
import co.present.present.ViewModelFactory
import co.present.present.analytics.AmplitudeEvents
import co.present.present.extensions.*
import co.present.present.feature.CitiesViewModel
import co.present.present.feature.MainActivity
import co.present.present.feature.SignUpDialogActivity
import co.present.present.feature.activity.ActivityFeedFragment
import co.present.present.feature.common.OnUserAddFriendListener
import co.present.present.feature.profile.info.UserProfileButtons
import co.present.present.feature.profile.info.UserProfileViewModel
import co.present.present.feature.profile.joined.JoinedCirclesFragment
import co.present.present.location.LocationDataProvider
import co.present.present.model.CurrentUser
import co.present.present.model.User
import co.present.present.model.isAlso
import co.present.present.model.isNot
import co.present.present.view.badge
import com.google.android.material.tabs.TabLayout
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.header_profile.*
import javax.inject.Inject

open class UserProfileFragment : BaseFragment(), OnUserAddFriendListener,
        UserProfileButtons.OnProfileActionClickListener, MainActivity.Resettable {
    private val TAG = javaClass.simpleName

    @Inject lateinit var viewModelFactory: ViewModelFactory
    private lateinit var viewModel: UserProfileViewModel
    private lateinit var citiesViewModel: CitiesViewModel
    @Inject lateinit var locationDataProvider: LocationDataProvider

    private val userId: String by lazy { arguments!!.getString(User.USER) }

    override fun onProfileEditClicked(user: User, currentUser: CurrentUser) {
        baseActivity.slideOverFromBottom<EditProfileActivity>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        performInjection()
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()

        // Trying to fix a measurement bug in profile header app bar: https://www.pivotaltracker.com/story/show/157794658
        appbar.requestLayout()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        baseActivity.setSupportActionBar(toolbar)
        baseActivity.supportActionBar?.title = ""

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(UserProfileViewModel::class.java)
        citiesViewModel = ViewModelProviders.of(baseActivity, viewModelFactory).get(CitiesViewModel::class.java)
    }

    private var adapter: UserProfilePagerAdapter? = null

    override fun onStart() {
        super.onStart()
            disposable += viewModel.getUserProfileInfo(userId)
                    .compose(applyFlowableSchedulers())
                    .subscribeBy(
                            onError = { e ->
                                Log.e(TAG, "Error loading user profile from database", e)
                            },
                            onNext = { (user, currentUser, friendshipState) ->
                                userProfileName.text = user.name
                                userProfilePhoto.loadCircularImage(user.photo)

                                if (user.isNot(currentUser)) {
                                    baseActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
                                }

                                buttons.bind(currentUser, user, friendshipState, this, this)
                                bio.text = user.bio

                                if (adapter == null) {
                                    adapter = UserProfilePagerAdapter(user, currentUser, baseActivity, childFragmentManager)
                                    viewPager.adapter = adapter

                                    tabLayout.setupWithViewPager(viewPager)
                                    viewPager.offscreenPageLimit = 2

                                    tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
                                        override fun onTabReselected(tab: TabLayout.Tab?) {
                                            // TODO: scroll to top (this is already implemented)
                                        }

                                        override fun onTabUnselected(tab: TabLayout.Tab?) {}

                                        override fun onTabSelected(tab: TabLayout.Tab) {
                                            when (tab.position) {
                                                0 -> analytics.log(AmplitudeEvents.PROFILE_VIEW_CIRCLES)
                                                1 -> if (user.isAlso(currentUser)) analytics.log(AmplitudeEvents.ACTIVITY_VIEW)
                                            }
                                        }
                                    })
                                }
                            }
                    )

        disposable += viewModel.getCircleUnreadBadgeCount(userId)
                .compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = {},
                        onNext = { badgeCount ->
                            val circlesTitle = if (badgeCount > 0) { requireContext().badge(R.string.circles, badgeCount) } else string(R.string.circles)
                            tabLayout.getTabAt(0)!!.text = circlesTitle
                        }
                )

        disposable += viewModel.getFriendRequestBadgeCount(userId).compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = {},
                        onNext = { badgeCount ->
                            val friendsTitle = if (badgeCount > 0) { requireContext().badge(R.string.friends, badgeCount) } else string(R.string.friends)
                            tabLayout.getTabAt(2)!!.text = friendsTitle
                        }
                )
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            analytics.log(AmplitudeEvents.PROFILE_VIEW)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.user_profile, menu)
        viewModel.isUserBlockedOrCurrentUser(userId).compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = {},
                        onNext = { (isBlocked, isCurrentUser, user) ->
                            if (isCurrentUser) {
                                menu.findItem(R.id.overflow).subMenu.removeItem(R.id.block)
                            } else {
                                menu.findItem(R.id.overflow).subMenu.removeItem(R.id.settings)

                                val titleRes = if (isBlocked) R.string.blocked else R.string.block
                                menu.findItem(R.id.block).apply {
                                    setTitle(titleRes)
                                    setOnMenuItemClickListener { onUserBlockSelected(isBlocked, user); true }
                                }
                            }
                        }
                ).addTo(disposable)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun resetToInitialState() {
        adapter?.scrollToTop()
        appbar.setExpanded(true)
        viewPager.currentItem = 0
    }

    private fun onUserBlockSelected(isBlocked: Boolean, user: User) {
        if (!isBlocked) {
            dialog(message = string(R.string.block_confirm_dialog_message),
                    title = string(R.string.block_confirm_dialog_title, user.name),
                    positiveButtonText = string(R.string.ok),
                    negativeButtonText = string(R.string.cancel)) {
                toggleUserBlock(isBlocked)
                analytics.log(AmplitudeEvents.USER_BLOCK)
            }
        } else {
            toggleUserBlock(isBlocked)
        }
    }

    private fun toggleUserBlock(isBlocked: Boolean) {
        viewModel.toggleUserBlock(userId, isBlocked).compose(applyCompletableSchedulers())
                .subscribeBy(
                        onError = {
                            snackbar(R.string.generic_error)
                        },
                        onComplete = {
                            // Do nothing; Menu update is driven from database
                        }
                )
    }

    override fun onProfileShareClicked(user: User, currentUser: CurrentUser) {
        shareProfile(currentUser, user)
    }

    override fun onProfileInviteClicked(user: User, currentUser: CurrentUser) {
        baseActivity.launchAddFriends()
    }

    override fun onUserAddFriendClicked(item: Any, user: User, currentUser: CurrentUser?) {
        if (currentUser == null) {
            start<SignUpDialogActivity>()
        } else {
            viewModel.changeUserFriendship(user, currentUser.id)
                    .compose(applyCompletableSchedulers())
                    .subscribeBy(
                            onError = { Log.e(TAG, "Error adding friend", it); toast(R.string.network_error) },
                            onComplete = {
                                // do nothing
                            }
                    ).addTo(disposable)
        }
    }

    private fun shareProfile(currentUser: CurrentUser, user: User) {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, R.string.present_invite_title)
            val shareText = if (user.isAlso(currentUser)) {
                // "Join me on Present! [link]"
                getString(R.string.present_invite_template, user.link)
            } else {
                // "Join Janete on Present! [link]"
                getString(R.string.present_other_invite_template, user.firstName, user.link)
            }
            putExtra(Intent.EXTRA_TEXT, shareText)
            startActivity(Intent.createChooser(this, getString(R.string.profile_share_title)))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> baseActivity.finish()
            R.id.settings -> baseActivity.slideOverFromRight<SettingsActivity>()
        }
        return true
    }

    fun performInjection() {
        activityComponent.inject(this)
    }

    companion object {

        fun newInstance(userId: String): Fragment {
            val bundle = Bundle().apply { putString(User.USER, userId) }
            return UserProfileFragment().apply { arguments = bundle }
        }
    }

    class UserProfilePagerAdapter(val user: User, val currentUser: CurrentUser, val context: Context, fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

        private val fragments = mutableListOf<Fragment?>(null, null, null)

        override fun getItem(position: Int): Fragment {
            val fragment = when(position) {
                0 -> JoinedCirclesFragment.newInstance(user.id)
                1 -> if (user.isAlso(currentUser)) ActivityFeedFragment() else FriendsFragment.newInstance(user.id)
                else -> FriendsFragment.newInstance(user.id)
            }

            fragments[position] = fragment
            return fragment
        }

        override fun getCount() = if (user.isAlso(currentUser)) 3 else 2

        override fun getPageTitle(position: Int): CharSequence? {
            return context.string(when (position) {
                0 -> R.string.circles
                1 -> if (user.isAlso(currentUser)) R.string.notifications else R.string.friends
                else -> R.string.friends
            })
        }

        fun scrollToTop() {
            fragments.forEach {
                if (it != null && it is MainActivity.Scrollable) {
                    it.scrollToTop()
                }
            }
        }
    }
}
