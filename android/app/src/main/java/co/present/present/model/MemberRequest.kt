package co.present.present.model

import present.proto.MembershipRequest

data class MemberRequest(val user: User, val timestamp: Long, val isApproved: Boolean = false) {

    constructor(request: MembershipRequest): this(
            user = User(request.user),
            timestamp = request.timestamp)

}

