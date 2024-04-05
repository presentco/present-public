package co.present.present

import android.app.Activity
import android.util.Log
import co.present.present.extensions.applySingleSchedulers
import co.present.present.extensions.start
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.user.UserDataProvider
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import present.proto.Authorization

interface Synchronize {

    fun synchronize(activity: Activity, userDataProvider: UserDataProvider, getCurrentUser: GetCurrentUser): Disposable

}

class SynchronizeImpl: Synchronize {
    private val TAG = javaClass.simpleName

    override fun synchronize(activity: Activity, userDataProvider: UserDataProvider, getCurrentUser: GetCurrentUser): Disposable {
        return userDataProvider.synchronize()
                .compose(applySingleSchedulers())
                .subscribeBy(
                        onError = { e ->
                            Log.e(TAG, "Error trying synchronize() ", e)
                            // do nothing; let's just try to sync next time
                        },
                        onSuccess = { syncResponse ->
                            Log.d(TAG, "Synchronize() response was ${syncResponse.authorization.nextStep}")
                            when (syncResponse.authorization.nextStep) {
                                Authorization.NextStep.AUTHENTICATE -> {
                                    if (syncResponse.userProfile != null) redirectToLaunch(activity)
                                }
                                Authorization.NextStep.PROCEED -> return@subscribeBy
                                else -> redirectToLaunch(activity)
                            }
                        }
                )
    }

    private fun redirectToLaunch(activity: Activity) {
        activity.start<LaunchActivity>()
        activity.finish()
    }
}