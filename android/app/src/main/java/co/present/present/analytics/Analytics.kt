package co.present.present.analytics

import android.app.Activity
import android.app.Application
import co.present.present.config.FeatureDataProvider
import co.present.present.extensions.toast
import co.present.present.model.CurrentUser
import com.amplitude.api.Amplitude
import com.amplitude.api.Identify
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class Analytics @Inject constructor(val featureDataProvider: FeatureDataProvider,
                                    val application: Application) {

    fun initialize(activity: Activity) {
        Amplitude.getInstance().initialize(activity, featureDataProvider.amplitudeApiKey).enableForegroundTracking(activity.application)
    }

    fun setUser(currentUser: CurrentUser?) {
        Amplitude.getInstance().userId = currentUser?.id
    }

    fun setUserProperties(locationPermission: Boolean) {
        val identify = Identify().add(AmplitudeKeys.LOCATION_PERMISSION, if (locationPermission) AmplitudeValues.ALLOWED else AmplitudeValues.DENIED)
        Amplitude.getInstance().identify(identify)
    }

    fun log(eventName: String, vararg properties: EventProperty) {
        if (properties.isEmpty()) {
            Amplitude.getInstance().logEvent(eventName)
        } else {
            val eventProperties = JSONObject().apply {
                properties.forEach {
                    when (it) {
                        is SingleEventProperty -> put(it.key, it.value)
                        is ListEventProperty -> put(it.key, JSONArray(it.values))
                    }
                }
            }
            Amplitude.getInstance().logEvent(eventName, eventProperties)
        }

        if (featureDataProvider.debugAnalytics) {
            application.toast(eventName)
        }
    }

    fun log(eventName: String, key: String, value: String) {
        log(eventName, SingleEventProperty(key, value))
    }

    abstract class EventProperty(open val key: String)
    data class SingleEventProperty(override val key: String, val value: String) : EventProperty(key)
    data class ListEventProperty(override val key: String, val values: Collection<String>) : EventProperty(key)
}

