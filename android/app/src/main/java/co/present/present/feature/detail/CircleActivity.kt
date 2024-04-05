package co.present.present.feature.detail

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.lifecycle.ViewModelProviders
import co.present.present.BaseActivity
import co.present.present.R
import co.present.present.analytics.AmplitudeEvents
import co.present.present.analytics.AmplitudeKeys
import co.present.present.extensions.applyFlowableSchedulers
import co.present.present.extensions.slideOverFromRight
import co.present.present.extensions.snackbar
import co.present.present.feature.detail.info.CircleViewModel
import co.present.present.feature.detail.info.OnCircleActionClickListener
import co.present.present.feature.discovery.CircleJoinHandler
import co.present.present.feature.discovery.CircleMuteHandler
import co.present.present.feature.invite.AddToCircleActivity
import co.present.present.model.Chat
import co.present.present.model.Circle
import co.present.present.model.CurrentUser
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_circle.*
import kotlinx.android.synthetic.main.toolbar_circle.*

open class CircleActivity : BaseActivity(), OnCircleActionClickListener {

    private val TAG: String = javaClass.simpleName

    private lateinit var viewModel: CircleViewModel

    val circleId: String by lazy { intent.getStringExtra(Circle.ARG_CIRCLE) }
    private val chatId: String? by lazy { intent.getStringExtra(Chat.ARG_CHAT) }
    private lateinit var circleJoinHandler: CircleJoinHandler
    private lateinit var circleMuteHandler: CircleMuteHandler


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_circle)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(CircleViewModel::class.java)
        circleJoinHandler = CircleJoinHandler(viewModel, this)
        circleMuteHandler = CircleMuteHandler(viewModel, analytics, this)

        disposable += viewModel.refreshCircle(circleId)
        disposable += viewModel.getCircleInfo(circleId)
                .compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = { e -> Log.e(TAG, "Error getting circle from database, this should never happen", e) },
                        onNext = { (circle, currentUserOptional, location) ->
                            header.configure(currentUserOptional.value, circle, location, featureDataProvider, this, circleJoinHandler)
                        }
                )
        analytics.log(AmplitudeEvents.CIRCLE_VIEW, AmplitudeKeys.CIRCLE_ID, circleId)
        analytics.log(AmplitudeEvents.CIRCLE_CHAT_VIEW, AmplitudeKeys.CIRCLE_ID, circleId)
    }


    override fun onCircleInviteClicked(currentUser: CurrentUser?) {
        analytics.log(AmplitudeEvents.CIRCLE_TAP_INVITE_FRIENDS)

        doIfLoggedIn {
            startActivityForResult(AddToCircleActivity.newIntent(this, circleId), CircleActivity.ADD_FRIENDS_REQUEST)
        }
    }

    override fun onCircleShareClicked(circle: Circle) {
        analytics.log(AmplitudeEvents.CIRCLE_VIEW_SHARE_CHOICES)
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, circle.title)
            val shareText = getString(R.string.circle_share_template, circle.title, circle.url)
            putExtra(Intent.EXTRA_TEXT, shareText)
            startActivity(Intent.createChooser(this, getString(R.string.share_circle)))
        }
    }

    override fun onCircleDetailClicked() {
        slideOverFromRight(CircleDetailActivity.newIntent(this, circleId), CIRCLE_DETAIL_REQUEST)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

    override fun onCircleMuteClicked() {
        circleMuteHandler.onCircleMuteClicked(circleId)
    }

    override fun performInjection() {
        activityComponent.inject(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ADD_FRIENDS_REQUEST && resultCode == Activity.RESULT_OK) {
            snackbar(R.string.circle_add_success)
        } else if (requestCode == CIRCLE_DETAIL_REQUEST && resultCode == CircleDetailActivity.RESULT_DELETED) {
            finish()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        val ADD_FRIENDS_REQUEST = 1
        val CIRCLE_DETAIL_REQUEST = 2
        const val chatTabIndex = 0

        fun newIntent(context: Context, circleId: String, commentId: String? = null): Intent {
            val intent = Intent(context, CircleActivity::class.java)
            intent.putExtra(Circle.ARG_CIRCLE, circleId)
            commentId?.let { intent.putExtra(Chat.ARG_CHAT, it) }
            return intent
        }
    }
}
