package co.present.present.extensions

import android.app.NotificationManager
import android.app.Service
import android.content.Context

val Service.notificationManager: NotificationManager
        get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
