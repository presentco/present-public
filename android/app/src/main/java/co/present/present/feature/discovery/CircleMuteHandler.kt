package co.present.present.feature.discovery

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import co.present.present.R
import co.present.present.analytics.AmplitudeEvents
import co.present.present.analytics.Analytics
import co.present.present.extensions.applySingleSchedulers
import co.present.present.extensions.snackbar
import co.present.present.feature.detail.info.CircleViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy

class CircleMuteHandler(val viewModel: CircleViewModel,
                        val analytics: Analytics,
                        val activity: AppCompatActivity) : LifecycleObserver, OnCircleMuteClickListener {
    private val TAG = javaClass.simpleName
    private val disposable = CompositeDisposable()

    init {
        activity.lifecycle.addObserver(this)
    }

    override fun onCircleMuteClicked(circleId: String) {
        toggleNotifications(circleId)
    }

    private fun toggleNotifications(circleId: String) {
        disposable += viewModel.toggleCircleNotifications(circleId).compose(applySingleSchedulers())
                .subscribeBy(
                        onError = { activity.snackbar(R.string.network_error) },
                        onSuccess = { updatedCircle ->
                            val formatResId = if (updatedCircle.muted) R.string.mute_confirmation else R.string.unmute_confirmation
                            activity.snackbar(activity.getString(formatResId, updatedCircle.title))
                            analytics.log(AmplitudeEvents.CIRCLE_CHAT_MUTE)
                        }
                )
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun disconnectListener() {
        disposable.clear()
    }
}

interface OnCircleMuteClickListener {
    fun onCircleMuteClicked(circleId: String)
}