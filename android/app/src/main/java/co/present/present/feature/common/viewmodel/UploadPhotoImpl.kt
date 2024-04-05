package co.present.present.feature.common.viewmodel

import android.net.Uri
import co.present.present.extensions.toByteString
import co.present.present.extensions.toImageBitmap
import co.present.present.service.Filesystem
import co.present.present.service.rpc.putContent
import com.facebook.FacebookSdk
import io.reactivex.Single
import okio.ByteString
import present.proto.ContentResponse
import present.proto.ContentService

abstract class UploadPhotoImpl(val contentService: ContentService, val filesystem: Filesystem): UploadPhoto {

    override val temporaryPhotoUri by lazy { filesystem.uriFromFile(temporaryPhotoFile) }
    override val temporaryPhotoFile by lazy { filesystem.fromFilesDir(temporaryPhotoFilename) }
    var photoUrl: String? = null

    private val tempPhotoAsByteString: Single<ByteString>
        get() = Single.fromCallable {
            temporaryPhotoUri.toImageBitmap(FacebookSdk.getApplicationContext().contentResolver)?.toByteString()
        }

    override fun uploadPhoto(): Single<Uri> {
        return tempPhotoAsByteString
                .flatMap {
                    contentService.putContent(it)
                            .doOnSuccess { contentResponse -> onContentResponse(contentResponse) }
                }.flatMap { Single.just(temporaryPhotoUri) }
    }

    fun onContentResponse(contentResponse: ContentResponse) {
        persistUuid(contentResponse.uuid)
        photoUrl = contentResponse.content
    }
}