package co.present.present.notifications

import android.util.Log
import co.present.present.PresentApplication
import co.present.present.extensions.applyCompletableSchedulers
import co.present.present.user.UserDataProvider
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import io.reactivex.rxkotlin.subscribeBy
import javax.inject.Inject


class FirebaseIdService: FirebaseInstanceIdService() {
    private val TAG: String = javaClass.simpleName
    @Inject lateinit var userDataProvider: UserDataProvider

    override fun onCreate() {
        super.onCreate()
        (application as PresentApplication).appComponent.inject(this)
    }

    override fun onTokenRefresh() {
        // Get updated InstanceID token.
        FirebaseInstanceId.getInstance().token?.let {
            Log.d(TAG, "Refreshed token: " + it)
            userDataProvider.firebaseToken = it
            userDataProvider.putDeviceToken(it).compose(applyCompletableSchedulers())
                    .subscribeBy(
                            onComplete = {
                                Log.d(TAG, "Token put successfully")
                            },
                            onError = { e ->
                                Log.e(TAG, "Token put failed", e)
                                // Can do retry later if saved token != user token
                            }
                    )
        }
    }
}