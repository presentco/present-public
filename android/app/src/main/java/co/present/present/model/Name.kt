package co.present.present.model

import androidx.room.Ignore
import present.proto.UserName

data class Name(var first: String, var last: String) {

    @Ignore
    constructor(userName: UserName?):this(userName?.first ?: "", userName?.last ?: "")
}