package co.present.present.feature.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import co.present.present.BaseActivity
import co.present.present.R
import co.present.present.extensions.applyFlowableSchedulers
import co.present.present.extensions.finishAndSlideBackOverToRight
import co.present.present.extensions.transaction
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.model.User
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_community.*
import kotlinx.android.synthetic.main.toolbar.*
import javax.inject.Inject


class FriendsActivity: BaseActivity() {
    private val TAG = javaClass.simpleName

    private val userId: String by lazy { intent.getStringExtra(User.USER) }
    @Inject lateinit var getCurrentUser: GetCurrentUser
    private lateinit var viewModel: FriendsViewModel

    override fun performInjection() {
        activityComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_community)
        viewModel = viewModelFactory.create(FriendsViewModel::class.java)

        setSupportActionBar(toolbar as Toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = ""
        }
        viewModel.getTitle(userId).compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = { Log.e(TAG, "error", it)},
                        onNext = {
                            toolbarTitle.text = it
                        }
                ).addTo(disposable)


        val tag = "friends"
        if (supportFragmentManager.findFragmentByTag(tag) == null) {
            supportFragmentManager.transaction {
                add(R.id.content, FriendsFragment.newInstance(userId), tag)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finishAndSlideBackOverToRight()
        return super.onOptionsItemSelected(item)
    }

    companion object {
        fun newIntent(context: Context, userId: String): Intent {
            return Intent(context, FriendsActivity::class.java).apply {
                putExtra(User.USER, userId)
            }
        }
    }
}

