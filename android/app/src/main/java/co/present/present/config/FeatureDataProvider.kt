package co.present.present.config

import android.content.SharedPreferences
import co.present.present.BuildConfig
import co.present.present.BuildConfig.FLAVOR
import co.present.present.extensions.preferences
import co.present.present.model.CurrentUser
import present.proto.Gender
import javax.inject.Inject

fun isInternalBuild() = FLAVOR == "internal"

class FeatureDataProvider @Inject constructor(val prefs: SharedPreferences) {

    private val defaultEndpoint = if (BuildConfig.DEBUG) Endpoint.Staging else Endpoint.Production
    var endpoint: Endpoint by preferences(prefs, defaultEndpoint, immediate = true)

    val serverUrl: String
    get() {
        return when (endpoint) {
            Endpoint.Staging -> BuildConfig.SERVER_URL_STAGING
            Endpoint.Production -> BuildConfig.SERVER_URL_RELEASE
            Endpoint.Custom -> "TODO"
        }
    }

    val serverBaseUrl: String
    get() {
        return when (endpoint) {
            Endpoint.Staging -> BuildConfig.SERVER_URL_BASE_STAGING
            Endpoint.Production -> BuildConfig.SERVER_URL_BASE
            Endpoint.Custom -> "TODO"
        }
    }

    val amplitudeApiKey: String
        get() {
            return when (endpoint) {
                Endpoint.Staging -> BuildConfig.AMPLITUDE_API_KEY_STAGING
                Endpoint.Production -> BuildConfig.AMPLITUDE_API_KEY_RELEASE
                Endpoint.Custom -> ""
            }
        }

    var overrideLocation: String? by preferences(prefs, null)
    var overrideLatitude: Double by preferences(prefs, 0.0)
    var overrideLongitude: Double by preferences(prefs, 0.0)

    val isLocationMocked get() = overrideLocation != null

    var overrideHomeUrl: Boolean by preferences(prefs, false, immediate = true)
    var homeUrl: String by preferences(prefs, "https://")

    var overrideNearbyFeedUrl: Boolean by preferences(prefs, false, immediate = true)
    var nearbyFeedUrl: String by preferences(prefs, "https://")

    var overrideGender: Int by preferences(prefs, -1, immediate = true)
    val shouldOverrideGender get() = overrideGender > -1

    fun canViewWomenOnly(currentUser: CurrentUser?): Boolean {
        if (currentUser == null) return false
        return if (shouldOverrideGender) overrideGender == Gender.WOMAN.value
        else currentUser.isAdmin || currentUser.gender != null && currentUser.gender == Gender.WOMAN.value
    }

    var debugAnalytics: Boolean by preferences(prefs, false, immediate = true)

}
