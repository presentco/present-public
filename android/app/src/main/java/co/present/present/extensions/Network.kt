package co.present.present.extensions

import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.Request

fun getHtml(url: String): Single<String> {
    return Single.fromCallable {
        val request = Request.Builder()
                .url(url)
                .build()
        OkHttpClient().newCall(request).execute().body()?.string() ?: ""
    }
}