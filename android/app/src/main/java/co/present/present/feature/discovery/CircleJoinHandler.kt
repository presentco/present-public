package co.present.present.feature.discovery

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import co.present.present.R
import co.present.present.extensions.*
import co.present.present.feature.SignUpDialogActivity
import co.present.present.feature.common.item.OnCircleJoinClickListener
import co.present.present.feature.detail.info.JoinCircle
import co.present.present.model.Circle
import co.present.present.model.CurrentUser
import co.present.present.model.isOwner
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy

class CircleJoinHandler(val viewModel: JoinCircle,
             val activity: AppCompatActivity) : LifecycleObserver, OnCircleJoinClickListener {
    private val TAG = javaClass.simpleName
    private val disposable = CompositeDisposable()

    init {
        activity.lifecycle.addObserver(this)
    }

    override fun onCircleJoinClicked(circle: Circle, currentUser: CurrentUser?) {
        if (currentUser == null) {
            activity.start<SignUpDialogActivity>()
            return
        }
        
        if (currentUser.isOwner(circle) && circle.joined) {
            activity.toast(R.string.owner_leave)
            return
        }

        if (circle.joined) {
            activity.dialog(message = R.string.leave_circle_dialog_message,
                    title = R.string.leave_circle_dialog_title,
                    positiveButtonText = R.string.leave,
                    onPositive = { toggleCircleJoin(circle) },
                    negativeButtonText = R.string.cancel)
        } else {
            toggleCircleJoin(circle)
        }
    }

    private fun toggleCircleJoin(circle: Circle) {
        disposable += viewModel.toggleCircleJoin(circle)
                .compose(applyCompletableSchedulers())
                .subscribeBy(
                        onError = {
                            Log.e(TAG, "error changing circle status", it)
                            val action = activity.getString(if (circle.joined) R.string.leave_action else R.string.join_action)
                            activity.snackbar(activity.getString(R.string.toggle_join_error, action))
                        },
                        onComplete = {
                            Log.d(TAG, "Successfully changed circle status ${circle.title}")
                            // No need to do anything; a database update will make the UI update
                        }
                )
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun disconnectListener() {
        disposable.clear()
    }
}