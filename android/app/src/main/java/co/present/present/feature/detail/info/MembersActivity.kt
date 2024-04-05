package co.present.present.feature.detail.info

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.present.present.BaseActivity
import co.present.present.R
import co.present.present.extensions.applyCompletableSchedulers
import co.present.present.extensions.applyFlowableSchedulers
import co.present.present.extensions.start
import co.present.present.feature.SignUpDialogActivity
import co.present.present.feature.common.OnUserAddFriendListener
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.model.Circle
import co.present.present.model.CurrentUser
import co.present.present.model.User
import co.present.present.view.DeleteTouchCallback
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_members.*
import javax.inject.Inject


class MembersActivity : BaseActivity(), OnUserAddFriendListener {

    private val TAG = javaClass.simpleName
    private val circleId: String by lazy { intent.getStringExtra(Circle.ARG_CIRCLE) }
    @Inject lateinit var getCurrentUser: GetCurrentUser
    private lateinit var viewModel: MembersViewModel
    private val touchCallback: DeleteTouchCallback by lazy {
        object : DeleteTouchCallback(this) {

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val item = adapter.getItem(viewHolder.adapterPosition)
                // Change notification to the adapter happens automatically when the section is
                // changed.
                if (item is UserRequestItem) {
                    viewModel.onRequestRemoved(item, circleId, item.user)
                }
            }
        }
    }
    private val adapter = GroupAdapter<ViewHolder>().apply { setOnItemClickListener(this@MembersActivity) }

    override fun performInjection() {
        activityComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_members)
        viewModel = viewModelFactory.create(MembersViewModel::class.java)


        setSupportActionBar(toolbar as Toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.members)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        ItemTouchHelper(touchCallback).attachToRecyclerView(recyclerView)

        viewModel.getItems(circleId, this).compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = { Log.d(TAG, "error", it)},
                        onNext = {
                            adapter.update(it)
                            recyclerView.scrollToPosition(0)
                        }
                ).addTo(disposable)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    override fun onUserAddFriendClicked(item: Any, user: User, currentUser: CurrentUser?) {
        if (currentUser == null) {
            start<SignUpDialogActivity>()
        } else {
            viewModel.changeUserFriendship(user, currentUser.id).compose(applyCompletableSchedulers())
                    .subscribeBy(
                            onError = {},
                            onComplete = {}
                    ).addTo(disposable)
        }
    }

    companion object {
        fun newIntent(context: Context, circleId: String): Intent {
            return Intent(context, MembersActivity::class.java).apply {
                putExtra(Circle.ARG_CIRCLE, circleId)
            }
        }
    }
}

