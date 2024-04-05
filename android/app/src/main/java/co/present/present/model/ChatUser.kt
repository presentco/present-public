package co.present.present.model

import co.present.present.model.CurrentUser
import present.proto.UserResponse

/**
 * Model for who posted to a chat
 */
data class ChatUser(val uuid: String,
                    val name: String,
                    val avatar: String) {

    constructor(userResponse: UserResponse): this(
        uuid = userResponse.id,
        name = userResponse.name,
        avatar = userResponse.photo
    )

    constructor(userResponse: CurrentUser): this(
        uuid = userResponse.id,
        name = userResponse.fullName,
        avatar = userResponse.photo
    )
}
