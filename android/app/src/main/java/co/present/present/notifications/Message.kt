package co.present.present.notifications

/**
 * Some constants to help with unpacking the map of extra data from server notifications.
 */
class Message {

    companion object {
        const val USER_ID = "userId"
        const val GROUP_ID = "groupId"
        const val COMMENT_ID = "commentId"
        const val TITLE = "title"
        const val BODY = "body"
        const val SOUND = "sound"
        const val SOUND_DISABLED = "disabled"
        const val SOUND_DEFAULT = "default"
        const val URL = "url"
    }
}