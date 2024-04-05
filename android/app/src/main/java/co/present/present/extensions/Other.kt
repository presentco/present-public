package co.present.present.extensions

import android.os.Build

// Not really sure what to call this file yet.

fun <T1 : Any, T2 : Any, R : Any> safeLet(p1: T1?, p2: T2?, block: (T1, T2) -> R?): R? {
    return if (p1 != null && p2 != null) block(p1, p2) else null
}

fun <T1 : Any, T2 : Any, T3 : Any, R : Any> safeLet(p1: T1?, p2: T2?, p3: T3?, block: (T1, T2, T3) -> R?): R? {
    return if (p1 != null && p2 != null && p3 != null) block(p1, p2, p3) else null
}

// Do func() only if specified API level or higher.
// Because lint can't see into this function to see it's safe, it should be written like this
// with an annotation on the lambda:
// fromApi(26) @TargetApi(26) { ... }
// If lint functionality improves, we can drop the target annotation.
fun fromApi(level: Int, func: () -> Unit) {
    if (Build.VERSION.SDK_INT >= level) func()
}