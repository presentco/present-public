package co.present.present.extensions

import android.os.Build

fun isEmulator(): Boolean {
    return Build.FINGERPRINT.contains("generic")
}