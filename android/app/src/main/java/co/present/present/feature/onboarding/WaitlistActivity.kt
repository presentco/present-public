package co.present.present.feature.onboarding

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.annotation.StringRes
import co.present.present.BaseActivity
import co.present.present.LaunchActivity
import co.present.present.R
import co.present.present.analytics.AmplitudeEvents
import co.present.present.analytics.AmplitudeKeys
import co.present.present.analytics.AmplitudeValues
import co.present.present.analytics.Analytics
import co.present.present.extensions.*
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_waitlist.*
import present.proto.Authorization
import java.util.concurrent.TimeUnit


open class WaitlistActivity : BaseActivity() {
    private val TAG: String = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waitlist)

        twitterButton.deepLinkSocial(
                R.string.social_twitter,
                R.string.twitter_package_name,
                eventType = AmplitudeValues.TWITTER
        )
        facebookButton.deepLinkSocial(R.string.social_facebook_deep_link,
                R.string.facebook_package_name, R.string.social_facebook,
                eventType = AmplitudeValues.FACEBOOK
        )
        instagramButton.deepLinkSocial(R.string.social_instagram_deep_link,
                R.string.instagram_package_name, R.string.social_instagram,
                eventType = AmplitudeValues.INSTAGRAM
        )

        button.setOnClickListener { inviteFriends() }

        analytics.log(AmplitudeEvents.SIGNUP_VIEW_WAIT_FOR_APPROVAL)
    }

    override fun onResume() {
        super.onResume()
        Flowable.interval(0, 5, TimeUnit.SECONDS)
                .flatMap { userDataProvider.synchronize().retry().toFlowable() }
                .doOnSubscribe {
                    // Use cached message to start, if we have one
                    userDataProvider.blockMessage?.let { setBlockMessage(it) }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { response ->
                    if (response.authorization.nextStep == Authorization.NextStep.BLOCK) {
                        Log.d(TAG, "Server response was ${response.authorization.nextStep.name}")
                        setBlockMessage(response.authorization.blockScreen.text)
                    }
                }
                .filter { it.authorization.nextStep == Authorization.NextStep.PROCEED }
                .subscribeBy(
                        onError = { e ->
                            Log.d(TAG, "Couldn't get authorization response from server", e)
                        },
                        onNext = { authorizationResponse ->
                            Log.d(TAG, "Server response was PROCEED, proceeding to app")
                            finish()
                            start(LaunchActivity::class.java)
                        }
                ).addTo(disposable)
    }

    private fun setBlockMessage(message: String) {
        description.text = message
        spinner.visibility = View.INVISIBLE
    }

    /**
     * Neither Facebook nor Instagram respect normal Android http deep linking.  By carefully
     * using particular explicit URLs and package names, we can get it to work (for now).
     *
     * There's no guarantee FB and Insta won't stop supporting these URLs in the future, but in
     * worst case they'll open in browser.
     *
     * Twitter deep links actually work fine, but while we're in here, may as well try explicitly
     * launching it as well to avoid the app chooser.
     */
    private fun ImageButton.deepLinkSocial(@StringRes deepLinkUrl: Int,
                                           @StringRes packageName: Int,
                                           @StringRes normalUrl: Int = deepLinkUrl,
                                           eventType: String) {
        setOnClickListener {
            Intent(ACTION_VIEW, uri(deepLinkUrl)).apply {
                `package` = getString(packageName)
                if (isCallable(this)) {
                    startActivity(this)
                    return@setOnClickListener
                }
            }
            launchUrl(normalUrl)
            analytics.log(AmplitudeEvents.SIGNUP_FOLLOW_SOCIAL_LINK,
                    Analytics.SingleEventProperty(AmplitudeKeys.SOCIAL_MEDIA, eventType))
        }
    }

    private fun inviteFriends() {
        userDataProvider.currentUserLocal.firstElement()
                .compose(applyMaybeSchedulers())
                .subscribeBy(
                        onError = { e ->
                            Log.d(TAG, "Couldn't get user in WaitlistActivity, should be impossible")
                        },
                        onSuccess = { currentUser ->
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.present_invite_title))
                                putExtra(Intent.EXTRA_TEXT, getString(R.string.present_invite_template, currentUser.appShareUrl))
                                start(Intent.createChooser(this, getString(R.string.invite_friends)))
                            }
                        }
                ).addTo(disposable)
    }

    override fun performInjection() {
        activityComponent.inject(this)
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }
}