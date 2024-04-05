package co.present.present.extensions

import android.annotation.SuppressLint
import android.content.SharedPreferences
import kotlin.reflect.KProperty

open class SharedPreferenceDelegate<T>(
        private val sharedPreferences: SharedPreferences,
        private val defaultValue: T,
        private val key: String? = null,
        private val immediate: Boolean = false) {

    private var loaded = false
    private var value: T = defaultValue

    private val KProperty<*>.keyName get() = key ?: fallbackName
    private val KProperty<*>.fallbackName: String
        get() = ownerCanonicalName?.let { "$it::$name" } ?: name

    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    operator open fun getValue(ref: Any?, property: KProperty<*>): T {
        if (!loaded) {
            value = when (defaultValue) {
                is String?, is String -> sharedPreferences.getString(property.keyName, defaultValue as String?)
                is Int -> sharedPreferences.getInt(property.keyName, defaultValue)
                is Double -> sharedPreferences.getFloat(property.keyName, defaultValue.toFloat())
                is Float -> sharedPreferences.getFloat(property.keyName, defaultValue)
                is Boolean -> sharedPreferences.getBoolean(property.keyName, defaultValue)
                is Set<*> -> sharedPreferences.getStringSet(property.keyName, defaultValue as Set<String>)
                else -> throw NotImplementedError()
            } as T
            loaded = true
        }
        return value
    }

    @SuppressLint("ApplySharedPref")
    operator open fun setValue(ref: Any?, property: KProperty<*>, value: T) {
        this.value = value
        val edits = sharedPreferences.edit().apply {
            when (value) {
                is String, is String? -> putString(property.keyName, value as String?)
                is Int -> putInt(property.keyName, value)
                is Double -> putFloat(property.keyName, value.toFloat())
                is Float -> putFloat(property.keyName, value)
                is Boolean -> putBoolean(property.keyName, value)
                is Set<*> -> {
                    // We can't assert it's of parameterized type Set<String> because of type
                    // erasure, so let's avoid an unchecked cast
                    val strings = value.filterIsInstance(String::class.java)
                    if (strings.size == value.size) {
                        putStringSet(property.keyName, strings.toSet())
                    } else {
                        throw NotImplementedError()
                    }
                }
                else -> throw NotImplementedError()
            }
        }

        if (immediate) edits.commit() else edits.apply()
    }
}

class ConverterSharedPreferenceDelegate<T, R>(sharedPreferences: SharedPreferences,
                                              defaultValue: R,
                                              private val toPrefs: (R) -> T,
                                              private val fromPrefs: (T) -> R,
                                              key: String? = null,
                                              immediate: Boolean = false,
                                              private val delegate: SharedPreferenceDelegate<T> = preferences(sharedPreferences, toPrefs(defaultValue), key, immediate)) {

    operator fun getValue(ref: Any?, property: KProperty<*>): R = fromPrefs(delegate.getValue(ref, property))

    operator fun setValue(ref: Any?, property: KProperty<*>, value: R) {
        delegate.setValue(ref, property, toPrefs(value))
    }
}

fun <T> preferences(prefs: SharedPreferences, defaultValue: T, key: String? = null, immediate: Boolean = false): SharedPreferenceDelegate<T> {
    return SharedPreferenceDelegate(prefs, defaultValue, key, immediate)
}

inline fun <reified E: Enum<E>> preferences(prefs: SharedPreferences, defaultValue: E, immediate: Boolean = false): ConverterSharedPreferenceDelegate<String, E> {
    return preferences(prefs, defaultValue, { value -> value.name }, { name -> enumValueOf(name) }, immediate = immediate)
}

fun <T, R> preferences(prefs: SharedPreferences, defaultValue: R, toPrefs: (R) -> T, fromPrefs: (T) -> R, key: String? = null, immediate: Boolean = false): ConverterSharedPreferenceDelegate<T, R> {
    return ConverterSharedPreferenceDelegate(prefs, defaultValue, toPrefs, fromPrefs, key, immediate = immediate)
}