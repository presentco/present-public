package co.present.present.feature

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import co.present.present.BaseActivity
import co.present.present.R
import co.present.present.analytics.Analytics
import co.present.present.config.FeatureDataProvider
import co.present.present.extensions.*
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.location.LocationDataProvider
import co.present.present.model.CurrentUser
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_web.*
import javax.inject.Inject


abstract class WebScreen(context: Context, attributeSet: AttributeSet) : FrameLayout(context, attributeSet), LifecycleObserver, LayoutContainer {

    private val TAG = javaClass.simpleName

    @Inject lateinit var locationDataProvider: LocationDataProvider
    @Inject lateinit var featureDataProvider: FeatureDataProvider
    @Inject lateinit var analytics: Analytics
    protected val onStopDisposable = CompositeDisposable()
    protected val disposable = CompositeDisposable()
    @Inject lateinit var getCurrentUser: GetCurrentUser
    override val containerView: View = this
    private lateinit var activity: BaseActivity

    open fun initiate(activity: BaseActivity) {
        activity.activityComponent.inject(this)
        activity.lifecycle.addObserver(this)
        this.activity = activity
    }

    init {
        inflate(context, R.layout.fragment_web, this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        fromApi(21) @TargetApi(Build.VERSION_CODES.LOLLIPOP) {
            webView.settings.apply {
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                javaScriptEnabled = true
                domStorageEnabled = true
            }
        }

        swipeRefreshLayout.setOnRefreshListener { softRefresh() }
        swipeRefreshLayout.isNestedScrollingEnabled = true

        // This is a fix for the web page itself having only an internal scrolling container and
        // not reporting its true scroll position.
        // I don't understand how it works, but it seems to work great!
        // https://stackoverflow.com/questions/29134082/webview-getscrolly-always-returns-0
        webView.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> if (view.scrollY <= 0) {
                    view.scrollTo(0, 1)
                }
            }
            false
        }

        // Don't let the swipe refresh layout pull down unless the web page is scrolled to top
        // (otherwise scrolling back up is impossible)
        swipeRefreshLayout.viewTreeObserver.addOnScrollChangedListener {
            swipeRefreshLayout?.isEnabled = webView.scrollY == 0
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun connect() {
        getCurrentUser.currentUserOptional.firstOrError().compose(applySingleSchedulers())
                .subscribeBy(
                        onError = {
                            Log.d(TAG, "error", it)
                        },
                        onSuccess = { webView.webViewClient = MyWebViewClient(it.value) }
                ).addTo(disposable)

        onStopDisposable += locationDataProvider.getLocation(context!!)
                .flatMap { getHtml().subscribeOn(Schedulers.io()) }
                .compose(applySingleSchedulers())
                .doOnSubscribe { if (html == null) spinner.show() }
                .subscribeBy(
                        onError = { e ->
                            Log.e(TAG, "Error loading web page", e)
                            spinner.hide()
                            //if (html == null) //TODO show visual error inline in page
                        },
                        onSuccess = { html ->
                            if (this.html != html) {
                                this.html = html
                                loadHtml(html)
                            }
                        }
                )
    }

    private var html: String? = null

    protected open fun onUrlClicked(url: String, currentUser: CurrentUser?) {
        Log.d(TAG, "Url clicked: $url")
        activity.start(Intent(activity, MainActivity::class.java).apply { data = Uri.parse(url) })
    }

    private inner class MyWebViewClient(val currentUser: CurrentUser?) : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            onUrlClicked(url, currentUser)
            return true
        }

        override fun onPageCommitVisible(view: WebView?, url: String?) {
            super.onPageCommitVisible(view, url)
            Log.d(TAG, "Page loaded successfully, in ${(System.currentTimeMillis() - startLoadTime) / 1000f}s")
            showLoadedWebView()
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            if (Build.VERSION.SDK_INT < 23) {
                Log.d(TAG, "Reached onStart, it's a bit early but best we can do for <23.  in ${(System.currentTimeMillis() - startLoadTime) / 1000f}s")
                showLoadedWebView()
            }
        }
    }

    private fun showLoadedWebView() {
        spinner?.hide()
        swipeRefreshLayout.isRefreshing = false
        webView?.show()
    }

    private fun softRefresh() {
        disposable += getHtml()
                .compose(applySingleSchedulers())
                .subscribeBy(
                        onError = { e ->
                            Log.e(TAG, "Error loading web page", e)
                            //snackbar(R.string.network_error)
                        },
                        onSuccess = { html ->
                            loadHtml(html)
                        }
                )
    }

    fun hardRefresh() {
        disposable += locationDataProvider.getLocation(context!!)
                .flatMap { getHtml().subscribeOn(Schedulers.io()) }
                .compose(applySingleSchedulers())
                .doOnSubscribe {
                    webView?.hide()
                    spinner?.show()

                }
                .subscribeBy(
                        onError = { e ->
                            Log.e(TAG, "Error loading web page", e)
                            spinner?.hide()
                            //snackbar(R.string.network_error)
                        },
                        onSuccess = { html ->
                            loadHtml(html)
                        }
                )
    }

    private fun loadHtml(html: String) {
        startLoadTime = System.currentTimeMillis()
        if (featureDataProvider.overrideHomeUrl) {
            webView?.loadData(html, "text/html", "UTF-8")
        } else {
            webView?.loadDataWithBaseURL(featureDataProvider.serverBaseUrl, html, "text/html", "UTF-8", null)
        }
    }

    private var startLoadTime: Long = 0

    abstract fun getHtml(): Single<String>

    fun scrollToTop() {
        webView.scrollTo(0, 0)
        webView.loadUrl("javascript:window.scrollTo(0, 0)")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        disposable.clear()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun disconnectListener() {
        onStopDisposable.clear()
    }
}