package co.present.present.feature

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import co.present.present.LaunchActivity
import co.present.present.PresentApplication
import co.present.present.extensions.start
import co.present.present.feature.detail.CircleActivity
import co.present.present.feature.profile.UserProfileActivity
import co.present.present.model.Chat
import co.present.present.model.Circle
import co.present.present.model.User
import co.present.present.notifications.Message

/**
 * This activity has no UI. It just brings the existing task stack of our app (if any)
 * to the foreground as-is, analyzes the situation, and launches the appropriate screen for a notification.
 */
class NotificationActivity : Activity() {

    private val circleId: String? by lazy { intent.getStringExtra(Circle.ARG_CIRCLE) }
    private val chatId: String? by lazy { intent.getStringExtra(Chat.ARG_CHAT) }
    private val userId: String? by lazy { intent.getStringExtra(User.USER) }
    private val url: String? by lazy { intent.getStringExtra(Message.URL) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val notificationIntent: Intent? = when {
            circleId != null -> CircleActivity.newIntent(this, circleId!!, chatId)
            userId != null -> UserProfileActivity.newIntent(this, userId!!)
            url != null -> Intent(this, MainActivity::class.java).apply { data = Uri.parse(url) }
            else -> null
        }

        notificationIntent?.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION

        // Launch the actual desired activity for the notification
        if (isTaskRoot) {
            // The app was not running before, so create a stack with the main activity first.

            val i = Intent(this, LaunchActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(i)

            // If a new screen is requested, open it on top.
            notificationIntent?.let { start(it) }
        } else if (requestedScreenIsOnTopOfStack(notificationIntent)) {
            // The user is currently viewing the requested screen, or the requested screen is on top in the background.

            // Bring the requested screen to top and dispatch onNewIntent() to it so it can configure its own UI as desired.
            notificationIntent?.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            notificationIntent?.let { start(it) }
        } else {
            // The application is already running, and if a screen is requested, it's not currently on top of the stack.

            // Launch a new instance of the requested screen on top of the existing stack.
            notificationIntent?.let { start(it) }
        }

        // Now finish
        finish()
    }


    private fun requestedScreenIsOnTopOfStack(notificationIntent: Intent?) =
            notificationIntent != null && (application as PresentApplication).inCircle(circleId)

    companion object {
        fun newIntent(context: Context,
                      url: String?,
                      circleId: String?,
                      commentId: String? = null,
                      userId: String? = null): Intent {
            val intent = Intent(context, NotificationActivity::class.java)
            url?.let { intent.putExtra(Message.URL, it) }
            circleId?.let { intent.putExtra(Circle.ARG_CIRCLE, it) }
            commentId?.let { intent.putExtra(Chat.ARG_CHAT, it) }
            userId?.let { intent.putExtra(User.USER, it) }
            return intent
        }
    }
}