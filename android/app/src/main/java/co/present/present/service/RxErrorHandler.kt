package co.present.present.service

import android.util.Log
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.functions.Consumer


class RxErrorHandler : Consumer<Throwable> {
    override fun accept(t: Throwable) {
        val e = if (t is UndeliverableException) t.cause else t

        if (e is InterruptedException || t is UndeliverableException) {
            Log.e("RxErrorHandler", "Swallowed undeliverable or interrupted exception", e)
            return
        } else {
            Thread.currentThread().uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e)
        }
    }
}
