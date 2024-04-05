package co.present.present.feature

import android.content.Context
import android.graphics.drawable.StateListDrawable
import android.util.Log
import android.util.StateSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ViewModelProviders
import co.present.present.BaseActivity
import co.present.present.R
import co.present.present.ViewModelFactory
import co.present.present.extensions.applyFlowableSchedulers
import co.present.present.extensions.hide
import co.present.present.extensions.loadCircularImage
import co.present.present.extensions.show
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.bottom_nav.view.*
import javax.inject.Inject


class BottomNav(val activity: BaseActivity, val listener: BottomNavListener) : LifecycleObserver {
    private val TAG = javaClass.simpleName

    @Inject lateinit var viewModelFactory: ViewModelFactory
    private var viewModel: BottomNavViewModel
    private val disposable = CompositeDisposable()
    private val tabs by lazy { with(bottomNavigationView) { listOf(feedIcon, post, profileIcon) } }
    private var lastState: BottomNavState? = null

    private lateinit var bottomNavigationView: ConstraintLayout

    init {
        activity.lifecycle.addObserver(this)
        activity.activityComponent.inject(this)
        viewModel = ViewModelProviders.of(activity, viewModelFactory).get(BottomNavViewModel::class.java)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun connect() {
        bottomNavigationView = activity.findViewById(R.id.bottomNav)
        bottomNavigationView.feedIcon.setImageDrawable(
                NavIconSelector(activity, R.drawable.ic_home, R.drawable.ic_home_selected))
        bottomNavigationView.post.setImageDrawable(
                NavIconSelector(activity, R.drawable.ic_create, R.drawable.ic_create_selected))


        disposable += viewModel.getBottomNavState().compose(applyFlowableSchedulers())
                .filter {
                    val lastState = lastState
                    lastState == null || lastState::class != it::class || hasProfileImageChanged(lastState, it)
                }
                .distinctUntilChanged()
                .subscribeBy(
                        onError = { e ->
                            Log.e(TAG, "Error getting state update in bottom nav bar", e)
                        },
                        onNext = { state ->
                            Log.i(TAG, "State update in bottom nav bar $state")

                            if (lastState == null || lastState!!::class != state::class) {
                                listener.onCreateFragments(state)

                                // Before user logs in, tab 1 is inline.  Afterward, it is a modal (it can't be
                                // selected).  If it was selected, force user to tab 0.
                                if (state is BottomNavState.LoggedIn && selectedTabIndex == 1) selectTab(0, state)
                            }

                            tabs.forEachIndexed { index, tab -> tab.setOnClickListener(getClickListener(index, state)) }
                            styleProfileIcon(state)
                            lastState = state
                        }
                )

        disposable += viewModel.getProfileBadge().compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = {
                            Log.e(TAG, "Couldn't get badge count from database")
                        },
                        onNext = { count ->
                            bottomNavigationView.badge.visibility = if (count > 0) View.VISIBLE else View.INVISIBLE
                            bottomNavigationView.badge.text = count.toString()
                        }
                )

        refreshSelectionState()
    }

    private fun hasProfileImageChanged(lastState: BottomNavState?, it: BottomNavState): Boolean {
        return lastState is BottomNavState.LoggedIn && it is BottomNavState.LoggedIn && lastState.currentUser.photo != it.currentUser.photo
    }

    private fun styleProfileIcon(state: BottomNavState) {
        when {
            (state is BottomNavState.LoggedIn && state.currentUser.photo.isNotBlank()) -> {
                bottomNavigationView.profileImage.loadCircularImage(state.currentUser.photo)
                bottomNavigationView.profileRing.show()
            }
            else -> {
                bottomNavigationView.profileImage.setImageDrawable(
                        NavIconSelector(activity, R.drawable.ic_profile, R.drawable.ic_profile_selected))
                ImageViewCompat.setImageTintList(bottomNavigationView.profileImage, null)
                bottomNavigationView.profileRing.hide()
            }
        }
    }

    val selectedTabIndex get() = viewModel.selectedTabIndex

    private fun refreshSelectionState() {
        tabs.forEachIndexed { index, tab -> tab.isSelected = index == viewModel.selectedTabIndex }
    }

    private fun getClickListener(index: Int, state: BottomNavState): View.OnClickListener {
        return View.OnClickListener {
            selectTab(index, state)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun disconnectListener() {
        disposable.clear()
    }

    interface BottomNavListener {
        fun onBottomNavTabSelected(index: Int, bottomNavState: BottomNavState): Boolean
        fun onCreateFragments(bottomNavState: BottomNavState)
    }

    fun goBack() {
        selectTab(0, viewModel.bottomNavStateSubject.value)
    }

    private fun selectTab(index: Int, state: BottomNavState) {
        if (listener.onBottomNavTabSelected(index, state)) {
            viewModel.selectedTabIndex = index
            refreshSelectionState()
        }
    }

    class NavIconSelector(context: Context, default: Int, selected: Int): StateListDrawable() {

        init {
            addState(intArrayOf(android.R.attr.state_pressed), drawable(context, selected))
            addState(intArrayOf(android.R.attr.state_selected), drawable(context, selected))
            addState(StateSet.WILD_CARD, drawable(context, default))
        }

        fun drawable(context: Context, resId: Int) = AppCompatResources.getDrawable(context, resId)
    }
}