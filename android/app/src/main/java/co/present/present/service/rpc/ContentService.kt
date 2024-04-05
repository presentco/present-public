package co.present.present.service.rpc

import io.reactivex.Single
import okio.ByteString
import present.proto.ContentResponse
import present.proto.ContentService
import present.proto.ContentType
import present.proto.ContentUploadRequest
import java.util.*

fun ContentService.putContent(byteString: ByteString, contentType: ContentType = ContentType.JPEG): Single<ContentResponse> {
    return Single.fromCallable {
        with(ContentUploadRequest(
                UUID.randomUUID().toString(),
                contentType,
                byteString, null)) {
            this@putContent.putContent(this)
        }
    }
}