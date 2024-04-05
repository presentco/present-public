package co.present.present.notifications

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import co.present.present.PresentApplication
import co.present.present.R
import co.present.present.db.CircleDao
import co.present.present.extensions.applySingleSchedulers
import co.present.present.extensions.fromApi
import co.present.present.extensions.notificationManager
import co.present.present.extensions.string
import co.present.present.feature.NotificationActivity
import co.present.present.service.rpc.CircleService
import co.present.present.service.rpc.getCircle
import com.google.firebase.messaging.RemoteMessage
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import javax.inject.Inject


class FirebaseMessagingService : com.google.firebase.messaging.FirebaseMessagingService() {
    private val TAG: String = javaClass.simpleName
    @Inject
    lateinit var circleService: CircleService
    @Inject
    lateinit var circleDao: CircleDao

    override fun onCreate() {
        super.onCreate()
        (application as PresentApplication).appComponent.inject(this)

        fromApi(26) @TargetApi(26) {
            // We can create this channel safely multiple times; after the first, it's a no-op
            NotificationChannel(string(R.string.channel_id_general),
                    string(R.string.channel_name_general),
                    IMPORTANCE_HIGH).apply {
                description = getString(R.string.channel_description_general)
                enableVibration(true)
                notificationManager.createNotificationChannel(this)
            }

            NotificationChannel(string(R.string.channel_id_after_hours),
                    string(R.string.channel_name_after_hours),
                    IMPORTANCE_HIGH).apply {
                description = getString(R.string.channel_description_after_hours)
                // No sound, no vibration
                setSound(null, null)
                notificationManager.createNotificationChannel(this)
            }
        }

    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Received push notification: " + remoteMessage.from)

        val circleId = remoteMessage.data[Message.GROUP_ID]
        val chatId = remoteMessage.data[Message.COMMENT_ID]
        val userId = remoteMessage.data[Message.USER_ID]
        val url = remoteMessage.data[Message.URL]

        val title = remoteMessage.data[Message.TITLE]
        val body = remoteMessage.data[Message.BODY]
        val sound = remoteMessage.data[Message.SOUND] ?: Message.SOUND_DEFAULT

        if (!suppressNotification(circleId)) {
            sendNotification(circleId, chatId, userId, sound, title, body, url)
        }

        circleId?.let { updateCircleInBackground(it) }
    }

    private fun updateCircleInBackground(it: String): Disposable {
        return circleService.getCircle(it).map { circleDao.update(it) }.compose(applySingleSchedulers())
                .subscribeBy(
                        onError = { Log.e(TAG, "Couldn't perform background update of group from notification", it) },
                        onSuccess = { Log.d(TAG, "Updated circle in background") }
                )
    }

    private fun sendNotification(circleId: String?, chatId: String?, userId: String?, sound: String?, title: String?, body: String?, url: String?) {

        val intent = NotificationActivity.newIntent(this, circleId = circleId,
                commentId = chatId, userId = userId, url = url)
        val contentIntent = PendingIntent.getActivity(this, System.currentTimeMillis().toInt(), intent, 0)


        val channelId = string(if (sound == Message.SOUND_DEFAULT) R.string.channel_id_general else R.string.channel_id_after_hours)

        val builder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification_present)
                .setColor(ContextCompat.getColor(applicationContext, R.color.presentPurple))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .apply {
                    // This enables sound, light, and vibration for pre-API 26 (pre-notification channels
                    if (sound == Message.SOUND_DEFAULT) setDefaults(Notification.DEFAULT_ALL)
                }
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)

        fromApi(21) @TargetApi(21) {
            builder.setCategory(Notification.CATEGORY_MESSAGE)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun suppressNotification(circleId: String?): Boolean {
        return circleId != null && presentApplication.inCircle(circleId) && presentApplication.inForeground()
    }

    private val presentApplication get() = application as PresentApplication

}