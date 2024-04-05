package co.present.present.feature.detail.chat

import co.present.present.feature.common.viewmodel.UploadPhotoImpl
import co.present.present.service.Filesystem
import present.proto.ContentService
import javax.inject.Inject


class ChatUploadPhotoImpl @Inject constructor(contentService: ContentService,
                                              filesystem: Filesystem)
    : UploadPhotoImpl(contentService, filesystem) {
    override fun clearUuid() {
        uuid = null
        photoUrl = null
    }

    var uuid: String? = null

    override val temporaryPhotoFilename = "tempLocalChatPhoto.jpg"

    override fun persistUuid(uuid: String) {
        this.uuid = uuid
    }
}