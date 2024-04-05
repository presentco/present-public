package co.present.present.model

import co.present.present.BuildConfig
import org.threeten.bp.LocalDateTime
import present.proto.CommentResponse
import toLocalDateTime

/**
 * Model for a comment in a circle view.
 */

data class Chat(val id: String,
                val index: Int, // Its index in the list of all comments in a circle
                val comment: String,
                val createdAt: Long,
                val deleted: Boolean = false,
                val user: ChatUser,
                val photo: String?) {

    constructor(commentResponse: CommentResponse) : this(
            user = ChatUser(commentResponse.author),
            id = commentResponse.uuid,
            index = commentResponse.index,
            comment = commentResponse.comment,
            deleted = commentResponse.deleted,
            createdAt = commentResponse.creationTime,
            photo = commentResponse.content?.content
    )

    constructor(uuid: String, comment: String, profile: CurrentUser, photo: String? = null) : this(
            user = ChatUser(profile),
            comment = comment,
            id = uuid,
            createdAt = System.currentTimeMillis(),
            index = -1,
            photo = photo
    )

    // A local version of the created date; this way a message with the timestamp 11pm in California
    // will fall on the current day's messages, whereas the same message in NYC would be in the
    // previous day and show at 2am
    val localCreatedDateTime: LocalDateTime get() = createdAt.toLocalDateTime()

    companion object {
        @JvmStatic val ARG_CHAT = "${BuildConfig.APPLICATION_ID}.Chat"
    }
}
