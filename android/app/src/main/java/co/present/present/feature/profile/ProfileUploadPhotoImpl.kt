package co.present.present.feature.profile

import co.present.present.feature.common.viewmodel.UploadPhotoImpl
import co.present.present.service.Filesystem
import co.present.present.user.UserDataProvider
import present.proto.ContentService
import javax.inject.Inject


class ProfileUploadPhotoImpl @Inject constructor(contentService: ContentService,
                                                 filesystem: Filesystem,
                                                 private val userPrefs: UserDataProvider)
    : UploadPhotoImpl(contentService, filesystem) {

    override val temporaryPhotoFilename = "tempLocalUserPhoto.jpg"

    override fun persistUuid(uuid: String) {
        userPrefs.photoUuid = uuid
    }

    override fun clearUuid() {
        userPrefs.photoUuid = null
    }
}