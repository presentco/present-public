package co.present.present.feature.common.viewmodel

import android.net.Uri
import io.reactivex.Single
import java.io.File

interface UploadPhoto {

    // Temporary local photo (while user is choosing photo locally and before it has been
    // saved on the server)
    val temporaryPhotoFilename: String

    val temporaryPhotoUri: Uri

    val temporaryPhotoFile : File

    fun uploadPhoto(): Single<Uri>

    fun persistUuid(uuid: String)

    fun clearUuid()

}