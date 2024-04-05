package co.present.present.extensions

import android.content.SharedPreferences

fun SharedPreferences.put(func: SharedPreferences.Editor.() -> SharedPreferences.Editor) {
    edit().func().apply()
}