package co.present.present

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.multidex.MultiDexApplication
import co.present.present.di.AppComponent
import co.present.present.di.AppModule
import co.present.present.di.DaggerAppComponent
import co.present.present.di.ServiceModule
import co.present.present.feature.detail.CircleActivity
import co.present.present.service.RxErrorHandler
import co.present.present.view.ActivityTracker
import co.present.present.view.ActivityVisibleCallbacks
import com.crashlytics.android.Crashlytics
import com.facebook.FacebookSdk
import com.facebook.stetho.Stetho
import com.jakewharton.threetenabp.AndroidThreeTen
import io.fabric.sdk.android.Fabric
import io.reactivex.plugins.RxJavaPlugins
import jonathanfinerty.once.Once

open class PresentApplication : MultiDexApplication(), Application.ActivityLifecycleCallbacks by ActivityVisibleCallbacks, ActivityTracker by ActivityVisibleCallbacks {
    private val TAG = javaClass.simpleName

    protected open val appModule: AppModule
        get() = AppModule(this)

    val appComponent: AppComponent
        get() = staticAppComponent

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
        staticAppComponent = createAppComponent()
        FacebookSdk.setApplicationId(getString(R.string.facebook_app_id))
        FacebookSdk.sdkInitialize(this)
        Once.initialise(this)
        AndroidThreeTen.init(this)
        RxJavaPlugins.setErrorHandler(RxErrorHandler())
        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
        }
        registerActivityLifecycleCallbacks(this)

        cleanupLegacyDatabase()
    }

    private fun cleanupLegacyDatabase() {
        if (databaseList().contains(ServiceModule.LEGACY_DB_FILE_NAME)) {
            Log.d(TAG, "Found legacy database, deleting ...")
            deleteDatabase(ServiceModule.LEGACY_DB_FILE_NAME)
            Log.d(TAG, "Legacy database deleted")
        }
    }

    override fun onTerminate() {
        unregisterActivityLifecycleCallbacks(this)
        super.onTerminate()
    }

    override fun onActivityStarted(activity: Activity) {
        ActivityVisibleCallbacks.onActivityStarted(activity)
        topActivity = activity
        Log.d(TAG, "Top activity is : ${activity.javaClass.simpleName}")
    }

    var topActivity: Activity? = null
        private set

    fun inCircle(circleId: String?): Boolean {
        (topActivity as? CircleActivity)?.let {
            return it.circleId == circleId
        }
        return false
    }

    protected open fun createAppComponent(): AppComponent {
        return DaggerAppComponent.builder()
                .appModule(appModule)
                .build()
    }

    companion object {
        @JvmStatic lateinit var staticAppComponent: AppComponent
            private set
    }


}
