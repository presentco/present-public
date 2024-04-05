package co.present.present.view

import android.app.Activity
import android.app.Application
import android.os.Bundle

object ActivityVisibleCallbacks : Application.ActivityLifecycleCallbacks, ActivityTracker {
    override fun inForeground(): Boolean {
        return numStarted > 0
    }

    override fun inBackground(): Boolean {
        return numStarted == 0
    }

    private var numStarted = 0


    override fun onActivityResumed(p0: Activity?) {}

    override fun onActivityPaused(p0: Activity?) {}

    override fun onActivityStarted(activity: Activity?) {
        if (numStarted == 0) {
            // app went to foreground
        }
        numStarted++
    }

    override fun onActivityStopped(activity: Activity?) {
        numStarted--
        if (numStarted == 0) {
            // app went to background
        }
    }

    override fun onActivityDestroyed(p0: Activity?) {}

    override fun onActivityCreated(p0: Activity?, p1: Bundle?) {}

    override fun onActivitySaveInstanceState(p0: Activity?, p1: Bundle?) {}

}

interface ActivityTracker {
    fun inForeground(): Boolean
    fun inBackground(): Boolean
}