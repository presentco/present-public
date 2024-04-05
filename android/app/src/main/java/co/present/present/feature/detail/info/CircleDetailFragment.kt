package co.present.present.feature.detail.info

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import co.present.present.BaseFragment
import co.present.present.R
import co.present.present.ViewModelFactory
import co.present.present.analytics.AmplitudeEvents
import co.present.present.analytics.AmplitudeKeys
import co.present.present.extensions.*
import co.present.present.feature.common.item.MemberItem
import co.present.present.feature.common.item.MemberRequestsItem
import co.present.present.feature.create.*
import co.present.present.feature.detail.CircleDetailActivity
import co.present.present.feature.discovery.CircleJoinHandler
import co.present.present.feature.discovery.CircleMuteHandler
import co.present.present.model.Circle
import co.present.present.model.CurrentUser
import co.present.present.user.UserDataProvider
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_circle_detail.*
import present.proto.FlagReason
import present.proto.GroupMemberPreapproval
import javax.inject.Inject


open class CircleDetailFragment : BaseFragment(), SwitchItem.OnSwitchChangedListener {

    private val TAG: String = javaClass.simpleName

    @Inject lateinit var userDataProvider: UserDataProvider
    @Inject lateinit var viewModelFactory: ViewModelFactory
    val circleId: String by lazy { activity!!.intent.getStringExtra(Circle.ARG_CIRCLE) }
    private lateinit var circleMuteHandler: CircleMuteHandler
    private lateinit var circleJoinHandler: CircleJoinHandler

    private val adapter: GroupAdapter<ViewHolder> by lazy {
        GroupAdapter<ViewHolder>().apply {
            setOnItemClickListener(this@CircleDetailFragment)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        performInjection()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_circle_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private lateinit var viewModel: CircleViewModel

    override fun onResume() {
        super.onResume()
        disposable += viewModel.getItems(circleId, this, this)
                .compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = { e ->
                            Log.e(TAG, "Error fetching circle from database, this should never happen", e)
                        },
                        onNext = { items ->
                            adapter.update(items)
                        }
                )
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(CircleViewModel::class.java)
        circleMuteHandler = CircleMuteHandler(viewModel, analytics, baseActivity)
        circleJoinHandler = CircleJoinHandler(viewModel, baseActivity)
    }

    override fun onSwitchChanged(item: SwitchItem, value: Boolean) {
        when (item) {
            is NotificationsSwitchItem -> circleMuteHandler.onCircleMuteClicked(circleId)
            is DiscoverabilitySwitchItem -> changeDiscoverability(value)
        }
    }

    private fun changeDiscoverability(value: Boolean) {
        viewModel.changeDiscoverability(value, circleId).compose(applyCompletableSchedulers()).subscribeBy(
                onError = { Log.e(TAG, "error", it); snackbar(R.string.generic_error) },
                onComplete = {
                    snackbar(if (value) R.string.circle_discoverable_confirm else R.string.circle_not_discoverable_confirm)
                }
        ).addTo(disposable)
    }

    override fun onItemClick(item: Item<*>, view: View) {
        val event = when (item) {
            is LocationItem -> AmplitudeEvents.CIRCLE_VIEW_LOCATION
            is MembersHeader -> AmplitudeEvents.CIRCLE_TAP_VIEW_ALL_MEMBERS
            is MemberItem, is MemberRequestsItem -> AmplitudeEvents.CIRCLE_VIEW_MEMBER
            else -> null
        }
        event?.let { analytics.log(it, AmplitudeKeys.CIRCLE_ID, circleId) }

        when (item) {
            is MemberRequestsItem -> baseActivity.launchMembers(circleId)
            is ActionablePreapprovalItem -> launchPreapprovalPicker(item.preapproval, item.womenOnly, PREAPPROVAL_REQUEST)
            is ReportItem -> reportCircle()
            is DeleteItem -> deleteCircle()
            is LeaveItem -> leaveCircle(item.circle, item.currentUser)
        }
        super.onItemClick(item, view)
    }

    private fun reportCircle() {
        dialog(title = R.string.report_circle_prompt, items = R.array.report_reasons, onItemClick = { dialogInterface, i ->
            val flagReason = when (i) {
                0 -> FlagReason.INAPPROPRIATE
                else -> FlagReason.SPAM
            }
            disposable += viewModel.reportCircle(circleId, flagReason).compose(applyCompletableSchedulers())
                    .subscribeBy(
                            onError = { e ->
                                snackbar(R.string.network_error)
                                Log.d(TAG, "Error submitting report", e)
                            },
                            onComplete = {
                                analytics.log(AmplitudeEvents.CIRCLE_CHAT_REPORT)
                                confirmCircleReported()
                            }
                    )
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            PREAPPROVAL_REQUEST -> {
                if (resultCode == Activity.RESULT_OK) {
                    changePreapproval(PreApproveActivity.fromIntent(data))
                }
            }
        }
    }

    private fun changePreapproval(newPreapproval: GroupMemberPreapproval) {
        viewModel.setPreapproval(newPreapproval, circleId).compose(applyCompletableSchedulers())
                .subscribeBy(
                        onError = { Log.e(TAG, "error", it); baseActivity.snackbar(R.string.generic_error) },
                        onComplete = {
                            // I don't think we need a snackbar here, because it's obvious the
                            // change was accepted because the chooser activity closed
                        }
                )
    }

    private fun confirmCircleReported() {
        dialog(title = R.string.report_submitted, positiveButtonText = R.string.ok)
    }

    private fun deleteCircle() {
        dialog(title = R.string.delete_circle_confirm, positiveButtonText = R.string.delete, negativeButtonText = R.string.cancel, onPositive = {
            disposable += viewModel.deleteCircle(circleId).compose(applyCompletableSchedulers())
                    .subscribeBy(
                            onError = { snackbar(R.string.network_error) },
                            onComplete = {
                                toast(R.string.delete_circle_success)
                                baseActivity.setResult(CircleDetailActivity.RESULT_DELETED)
                                baseActivity.finish()
                            }
                    )
        })
    }

    private fun leaveCircle(circle: Circle, currentUser: CurrentUser?) {
        circleJoinHandler.onCircleJoinClicked(circle, currentUser)
    }

    private fun launchPreapprovalPicker(preapproval: GroupMemberPreapproval, womenOnly: Boolean, requestCode: Int) {
        slideOverFromRight(PreApproveActivity.newIntent(requireContext(), preapproval, womenOnly), requestCode)
    }

    protected open fun performInjection() {
        activityComponent.inject(this)
    }

    companion object {
        private const val PREAPPROVAL_REQUEST = 1

        fun newInstance(circleId: String): CircleDetailFragment {
            val fragment = CircleDetailFragment()
            val bundle = Bundle().apply { putString(Circle.ARG_CIRCLE, circleId) }
            fragment.arguments = bundle
            return fragment
        }
    }
}
