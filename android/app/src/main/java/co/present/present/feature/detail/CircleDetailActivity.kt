package co.present.present.feature.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProviders
import co.present.present.BaseActivity
import co.present.present.R
import co.present.present.analytics.AmplitudeEvents
import co.present.present.analytics.AmplitudeKeys
import co.present.present.extensions.applyFlowableSchedulers
import co.present.present.extensions.start
import co.present.present.feature.create.EditCircleActivity
import co.present.present.feature.detail.info.CircleViewModel
import co.present.present.feature.discovery.CircleJoinHandler
import co.present.present.model.Circle
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_circle_detail.*
import kotlinx.android.synthetic.main.toolbar_circle.*

open class CircleDetailActivity : BaseActivity() {
    private val TAG: String = javaClass.simpleName

    private lateinit var viewModel: CircleViewModel

    val circleId: String by lazy { intent.getStringExtra(Circle.ARG_CIRCLE) }
    private lateinit var circleJoinHandler: CircleJoinHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_circle_detail)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(CircleViewModel::class.java)
        circleJoinHandler = CircleJoinHandler(viewModel, this)

        disposable += viewModel.refreshCircle(circleId)
        disposable += viewModel.getCircleInfo(circleId)
                .compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = { e -> Log.e(TAG, "Error getting circle from database, this should never happen", e) },
                        onNext = { (circle, currentUserOptional, location) ->
                            header.configure(circle, location, featureDataProvider)
                        }
                )
        analytics.log(AmplitudeEvents.CIRCLE_VIEW_INFO, AmplitudeKeys.CIRCLE_ID, circleId)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.edit -> editCircle()
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.circle_detail, menu)
        viewModel.getEditEnabled(circleId).compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = { Log.e(TAG, "error", it)},
                        onNext = {
                            if (it) {
                                var edit: MenuItem? = menu.findItem(R.id.edit)
                                if (edit == null) {
                                    edit = menu.add(0, R.id.edit, 0, R.string.edit)
                                    edit.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                                }
                            } else {
                                menu.removeItem(R.id.edit)
                            }
                        }).addTo(disposable)
        return super.onCreateOptionsMenu(menu)
    }

    private fun editCircle() {
        start(EditCircleActivity.newIntent(this, circleId))
    }

    override fun performInjection() {
        activityComponent.inject(this)
    }

    companion object {

        fun newIntent(context: Context, circleId: String): Intent {
            val intent = Intent(context, CircleDetailActivity::class.java)
            intent.putExtra(Circle.ARG_CIRCLE, circleId)
            return intent
        }

        val RESULT_DELETED = 7894
    }
}
