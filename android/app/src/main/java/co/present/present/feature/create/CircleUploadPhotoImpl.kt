package co.present.present.feature.create

import co.present.present.feature.common.viewmodel.UploadPhotoImpl
import co.present.present.service.Filesystem
import present.proto.ContentService
import javax.inject.Inject


class CircleUploadPhotoImpl @Inject constructor(contentService: ContentService,
                                                filesystem: Filesystem)
    : UploadPhotoImpl(contentService, filesystem) {

    var uuid: String? = null

    override val temporaryPhotoFilename = "tempLocalCirclePhoto.jpg"

    override fun persistUuid(uuid: String) {
        this.uuid = uuid
    }

    override fun clearUuid() {
        uuid = null
    }
}