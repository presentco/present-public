package co.present.present

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import co.present.present.analytics.Analytics
import co.present.present.config.Endpoint
import co.present.present.config.FeatureDataProvider
import co.present.present.config.isInternalBuild
import co.present.present.db.CircleDao
import co.present.present.db.CurrentUserDao
import co.present.present.di.ActivityComponent
import co.present.present.di.ActivityModule
import co.present.present.di.ActivityScope
import co.present.present.di.AppComponent
import co.present.present.extensions.*
import co.present.present.feature.*
import co.present.present.feature.common.item.*
import co.present.present.feature.create.EditCircleActivity
import co.present.present.feature.detail.CircleActivity
import co.present.present.feature.detail.info.LocationItem
import co.present.present.feature.detail.info.MembersActivity
import co.present.present.feature.detail.info.MembersHeader
import co.present.present.feature.detail.info.UserRequestItem
import co.present.present.feature.invite.AddFriendsActivity
import co.present.present.feature.onboarding.OnboardingDataProvider
import co.present.present.feature.onboarding.PhoneLoginActivity
import co.present.present.feature.onboarding.step.FacebookLinkActivity
import co.present.present.feature.profile.UserProfileActivity
import co.present.present.location.launchGoogleMaps
import co.present.present.model.Category
import co.present.present.model.Circle
import co.present.present.model.User
import co.present.present.service.RpcManager
import co.present.present.service.rpc.putToken
import co.present.present.user.UserDataProvider
import co.present.present.view.AfterTextChangedWatcher
import co.present.present.view.OnLinkClickedListener
import com.facebook.AccessToken
import com.facebook.login.LoginManager
import com.google.android.material.navigation.NavigationView
import com.xwray.groupie.Item
import com.xwray.groupie.OnItemClickListener
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import jonathanfinerty.once.Once
import kotlinx.android.synthetic.main.debug_wrapper.*
import present.proto.Gender
import present.proto.GroupService
import present.proto.UserService
import javax.inject.Inject

@ActivityScope
abstract class BaseActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
        OnLinkClickedListener, OnItemClickListener {

    private val TAG = javaClass.simpleName

    @Inject lateinit var featureDataProvider: FeatureDataProvider
    @Inject lateinit var userDataProvider: UserDataProvider
    @Inject lateinit var currentUserDao: CurrentUserDao
    @Inject lateinit var debugCircleDao: CircleDao
    @Inject lateinit var groupService: GroupService
    @Inject lateinit var userService: UserService
    @Inject lateinit var rpcManager: RpcManager
    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var analytics: Analytics
    protected val disposable = CompositeDisposable()
    private lateinit var urlResolverViewModel: UrlResolverViewModel
    private lateinit var bottomNavViewModel: BottomNavViewModel
    @Inject lateinit var onboardingDataProvider: OnboardingDataProvider
    private lateinit var debugViewModel: DebugViewModel


    private val appComponent: AppComponent
        get() = (application as PresentApplication).appComponent

    val activityComponent: ActivityComponent
        get() = appComponent.plus(ActivityModule(this))

    private lateinit var citiesViewModel: CitiesViewModel



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        performInjection()
        analytics.initialize(this)
        urlResolverViewModel = ViewModelProviders.of(this, viewModelFactory).get(UrlResolverViewModel::class.java)
        citiesViewModel = ViewModelProviders.of(this, viewModelFactory).get(CitiesViewModel::class.java)
        bottomNavViewModel = ViewModelProviders.of(this, viewModelFactory).get(BottomNavViewModel::class.java)
        debugViewModel = ViewModelProviders.of(this, viewModelFactory).get(DebugViewModel::class.java)
    }

    override fun onItemClick(item: Item<*>, view: View) {
        when (item) {
            is FriendItem -> launchUser(item.user.id)
            is CircleItem -> launchCircle(item.circle.id)
            is SmallCircleItem -> launchCircle(item.circle.id)
            is CircleItemHorizontal -> launchCircle(item.circle.id)
            is LocationItem -> launchGoogleMaps(item.latitude, item.longitude, item.locationName)
            is UserItem -> launchUser(item.user.id)
            is UserRequestItem -> launchUser(item.user.id)
            is MembersHeader -> item.circle?.id?.let { launchMembers(it) }
        }
    }

    fun launchMembers(circleId: String) {
        doIfLoggedIn { start(MembersActivity.newIntent(this, circleId)) }
    }

    fun launchUser(userId: String) {
        doIfLoggedIn { start(UserProfileActivity.newIntent(this, userId)) }
    }

    fun doIfLoggedIn(action: () -> Unit) {
        if (bottomNavViewModel.isLoggedIn()) {
            action()
        } else {
            start<SignUpDialogActivity>()
        }
    }

    fun doIfNotLoggedIn(action: () -> Unit) {
        if (!bottomNavViewModel.isLoggedIn()) {
            action()
        }
    }

    private fun launchCircle(circleId: String) {
        start(CircleActivity.newIntent(this, circleId))
    }

    private fun launchCircleJoinRequests(circleId: String) {
        start(MembersActivity.newIntent(this, circleId))
    }

    private fun launchCreateCircle(uri: Uri) {
        doIfLoggedIn {
            slideOverFromBottom(EditCircleActivity.newPrefillIntent(this, EditCircleActivity.PrefillInfo(uri)))
        }
    }

    private fun launchCityPicker() {
        slideOverFromRight<CityPickerActivity>()
    }

    protected open fun launchCategory(categoryName: String) {
        start(CategoryActivity.newIntent(this, category = categoryName))
    }

    fun launchAddFriends() {
        doIfLoggedIn {
            slideOverFromRight(Intent(this, AddFriendsActivity::class.java))
        }
    }

    protected abstract fun performInjection()

    override fun setContentView(layoutResID: Int) {
        if (showDebugDrawer && (BuildConfig.DEBUG || isInternalBuild() || userDataProvider.userProfile?.isAdmin == true)) {
            setDebugContentView(layoutResID)
        } else {
            super.setContentView(layoutResID)
        }
    }

    open val showDebugDrawer = true

    private fun setDebugContentView(layoutResID: Int) {
        // Inflate our own DrawerLayout and NavigationView instead of super's content
        super.setContentView(R.layout.debug_wrapper)
        debugDrawer.setNavigationItemSelectedListener(this)

        prepareNavigationMenu()

        // Inflate super's layoutResId into our own content frame
        layoutInflater.inflate(layoutResID, mainContent)
    }

    private fun invalidateNavigationMenu() {
        debugDrawer.menu.clear()
        debugDrawer.inflateMenu(R.menu.debug_drawer)
        prepareNavigationMenu()
    }

    private fun prepareNavigationMenu() {
        debugDrawer.menu.apply {
            findItem(R.id.endpoint).apply {
                title = "Endpoint: (${featureDataProvider.endpoint})"
            }

            setupHomeOverride()
            setupNearbyFeedOverride()
            setupLocationSwitcher()
            setupBlocklist()
            setupGenderOverride()
            setupDebugAnalytics()

            add("Version: ${BuildConfig.VERSION_NAME}").apply {
                isEnabled = false
            }
        }
    }

    private fun Menu.setupDebugAnalytics() {
        val debugAnalyticsItem = findItem(R.id.debugAnalytics)
        fun getTitle() = if (featureDataProvider.debugAnalytics) "Hide debug analytics" else "Show debug analytics"
        debugAnalyticsItem.title = getTitle()
        debugAnalyticsItem.setOnMenuItemClickListener { item ->
            featureDataProvider.debugAnalytics = !featureDataProvider.debugAnalytics
            item.title = getTitle()
            true
        }
    }

    private fun Menu.setupGenderOverride() {
        debugViewModel.getGenderTitle().subscribeBy(
                onError = {},
                onNext = {
                    if (it.value == null) { removeItem(R.id.overrideGender) }
                    else {
                        val item = findItem(R.id.overrideGender) ?: add(R.id.overrideGender, R.id.overrideGender, 0, "")
                        item.title = it.value
                    }
                }
        )
    }

    private fun Menu.setupBlocklist() {
        debugViewModel.getBlockListTitle().compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = {},
                        onNext = {
                            if (it.value == null) { removeItem(R.id.blockList) }
                            else {
                                val item = findItem(R.id.blockList) ?: add(R.id.blockList, R.id.blockList, 0, "")
                                item.title = it.value
                            }
                        }
                )
    }

    private fun Menu.setupLocationSwitcher() {
        citiesViewModel.getCurrentCity().compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = {}, onNext = { cityOptional ->
                    findItem(R.id.locationSwitcher).apply {
                        title = cityOptional.value ?: "Current location"
                    }
                })

    }

    private fun Menu.setupHomeOverride() {
        findItem(R.id.overrideHomeUrl).apply {
            actionView.findViewById<Switch>(R.id.zwitch).apply {
                isChecked = featureDataProvider.overrideHomeUrl
                setOnCheckedChangeListener { compoundButton, checked ->
                    featureDataProvider.overrideHomeUrl = checked
                    invalidateNavigationMenu()
                }
            }
        }
        if (!featureDataProvider.overrideHomeUrl) {
            removeItem(R.id.homeUrl)
        } else {
            findItem(R.id.homeUrl).apply {
                actionView.findViewById<EditText>(R.id.editText).apply {
                    setText(featureDataProvider.homeUrl, TextView.BufferType.EDITABLE)
                    addTextChangedListener(object : AfterTextChangedWatcher() {
                        override fun afterTextChanged(editable: Editable) {
                            featureDataProvider.homeUrl = editable.toString()
                        }
                    })
                }
            }
        }
    }

    private fun Menu.setupNearbyFeedOverride() {
        findItem(R.id.overrideNearbyFeedUrl).apply {
            actionView.findViewById<Switch>(R.id.zwitch).apply {
                isChecked = featureDataProvider.overrideNearbyFeedUrl
                setOnCheckedChangeListener { compoundButton, checked ->
                    featureDataProvider.overrideNearbyFeedUrl = checked
                    invalidateNavigationMenu()
                }
            }
        }
        if (!featureDataProvider.overrideNearbyFeedUrl) {
            removeItem(R.id.nearbyFeedUrl)
        } else {
            findItem(R.id.nearbyFeedUrl).apply {
                actionView.findViewById<EditText>(R.id.editText).apply {
                    setText(featureDataProvider.nearbyFeedUrl, TextView.BufferType.EDITABLE)
                    addTextChangedListener(object : AfterTextChangedWatcher() {
                        override fun afterTextChanged(editable: Editable) {
                            featureDataProvider.nearbyFeedUrl = editable.toString()
                        }
                    })
                }
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.endpoint -> showEndpointDialog()
            R.id.clearCircles -> clearCirclesAndRestart()
            R.id.clearOnce -> clearOnce()
            R.id.locationSwitcher -> launchCityPicker()
            R.id.logOut -> logOut()
            R.id.blockList -> clearBlockList()
            R.id.overrideGender -> showGenderDialog()
        }

        return true
    }

    private fun showGenderDialog() {
        debugViewModel.currentUser.firstOrError().compose(applySingleSchedulers())
                .subscribeBy(onError = {}, onSuccess = {
                    val genders = Gender.values().map { it.toString() }
                    val gendersPlusDefault = genders.toMutableList().apply {
                        add(0, "Default: ${Gender.fromValue(it.gender ?: 0)}")
                    }

                    arrayDialog("Override Gender", gendersPlusDefault) { genderString ->
                        val selectedGender = genders.indexOf(genderString)
                        debugViewModel.setGenderOverride(selectedGender)
                        triggerRebirth()
                    }
                })
    }

    private fun clearBlockList() {
        debugViewModel.clearBlockList().subscribeBy(
                onError = { Log.e(TAG, "error", it) },
                onComplete = {
                    // Nothing necessary, menu should update itself
                }
        )
    }

    private fun clearOnce() {
        Once.clearAll()
        triggerRebirth()
    }

    internal fun clearCirclesAndRestart() {
        Completable.fromCallable { debugCircleDao.clear() }
                .compose(applyCompletableSchedulers())
                .subscribe { triggerRebirth() }
    }

    private fun showEndpointDialog() {
        arrayDialogFromEnum<Endpoint> { selectedEndpoint ->
            featureDataProvider.endpoint = selectedEndpoint

            unregisterForNotifications {
                        clearAppData()
                        triggerRebirth()
                    }
        }
    }

    private fun unregisterForNotifications(onComplete: () -> Unit) {
        userService.putToken(null).compose(applyCompletableSchedulers()).subscribeBy(
                onError = { e ->
                    Log.e(TAG, "kjhkjh", e)
                    toast("Couldn't unregister your old notification token. " +
                        "Please try again")},
                onComplete = onComplete
        )
    }

    protected fun logOut() {
        unregisterForNotifications {
            clearAppData()
            clearCirclesAndRestart()
        }
    }

    private inline fun <reified E : Enum<E>> arrayDialogFromEnum(title: String = E::class.java.simpleName, crossinline onSelected: (E) -> Unit) {
        AlertDialog.Builder(this).apply {
            setTitle(title)
            val names = enumValues<E>().map { it.name }.toTypedArray()

            setItems(names, { _, selectedIndex ->
                enumValues<E>()[selectedIndex].let { selectedValue ->
                    onSelected.invoke(selectedValue)
                }
            })
            show()
        }
    }

    private fun arrayDialog(title: String, items: List<String>, onSelected: (String) -> Unit) {
        AlertDialog.Builder(this).apply {
            setTitle(title)
            setItems(items.toTypedArray(), { _, selectedIndex ->
                onSelected.invoke(items[selectedIndex])
            })
            show()
        }
    }

    private fun clearAppData() {
        userDataProvider.clear()
        bottomNavViewModel.clear()
        onboardingDataProvider.clear()
        rpcManager.clear()
        clearCurrentUserTable()
        clearCirclesAndRestart()
        logOutOfFacebook()
    }

    private fun clearCurrentUserTable() {
        Completable.fromCallable { currentUserDao.clear() }
                .compose(applyCompletableSchedulers())
                .subscribe()
    }

    private fun logOutOfFacebook() {
        if (AccessToken.getCurrentAccessToken() != null) {
            LoginManager.getInstance().logOut()
        }
    }

    internal fun triggerRebirth() {
        // Hacky but, give the app a few moments to save any data we changed, then kill and rebirth
        debugDrawer.postDelayed({
            restart()

            // Kill the whole process, so the Application object is recreated
            Runtime.getRuntime().exit(0)
        }, 200)
    }

    protected fun restart() {
        val intent = Intent(this, LaunchActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        start(intent)
        finish()
    }

    // TODO: End debug stuff; all the debug stuff should go in its own class and get out of here

    override fun onLinkClick(uri: Uri) {
        launchDeepLink(uri)
    }

    fun launchDeepLink(uri: Uri) {
        Log.d(TAG, "Activity was launched with a deep link: $uri")

        when {
            uri.isAppLink() -> {
                if (uri.isCreateCircleLink()) {
                    launchCreateCircle(uri)
                } else if (uri.isTourLink()) {
                    // The "tour" is now just showing the logged out state
                } else if (uri.isChooseLocationLink()) {
                    launchCityPicker()
                } else if (uri.isAddFriendsLink()) {
                    launchAddFriends()
                } else if (uri.isLinkFacebookLink()) {
                    doIfLoggedIn { start<FacebookLinkActivity>() }
                } else if (uri.isLoginLink()) {
                    doIfNotLoggedIn { start<PhoneLoginActivity>() }
                }
            }
            uri.isValidShortLink() -> resolveShortLink(uri)
            else -> {
                Log.d(TAG, "Not a valid short link or app link, not trying to resolve, just sending to browser: $uri")
                launchUrl(uri.toString())
                return
            }
        }
    }

    private val spinner get() = findViewById<ProgressBar?>(R.id.spinner)

    private fun resolveShortLink(uri: Uri) {
        disposable += urlResolverViewModel.resolve(uri)
                .compose(applySingleSchedulers())
                //TODO
                //.doOnSubscribe { spinner?.show() }
                .subscribeBy(
                        onError = { e ->
                            Log.e(TAG, "Error resolving url: $uri", e)
                            spinner?.hide()
                        },
                        onSuccess = { any ->
                            Log.d(TAG, "Spinner is null? ${spinner == null}")
                            spinner?.hide()
                            Log.d(TAG, "Received object from url resolver: $any")
                            when (any) {
                                is Circle -> {
                                    launchCircle(any.id)
                                    if (uri.isCircleRequestsLink()) {
                                        launchCircleJoinRequests(any.id)
                                    }
                                }
                                is User -> launchUser(any.id)
                                is Category -> launchCategory(any.name)
                                else -> error("we should be in the error block now")
                            }
                        }
                )
    }

    override fun onDestroy() {
        disposable.clear()
        super.onDestroy()
    }
}
