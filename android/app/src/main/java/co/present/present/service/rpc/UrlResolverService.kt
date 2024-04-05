import android.net.Uri
import io.reactivex.Single
import present.proto.ResolveUrlRequest
import present.proto.ResolveUrlResponse
import present.proto.UrlResolverService

fun UrlResolverService.resolve(uri: Uri): Single<ResolveUrlResponse> {
    return resolve(uri.toString())
}

fun UrlResolverService.resolve(url: String): Single<ResolveUrlResponse> {
    return Single.fromCallable {
        resolveUrl(ResolveUrlRequest(url))
    }
}