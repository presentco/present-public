package co.present.present.model

enum class FriendshipState(val value: Int) {
    None(0), Requested(1), Accepted(2);

    companion object {
        fun fromFriendshipState(state: present.proto.FriendshipState?): FriendshipState {
            return when (state) {
                null -> None
                present.proto.FriendshipState.REQUESTED -> Requested
                else -> Accepted
            }
        }

        fun fromValue(value: Int): FriendshipState {
            return when(value) {
                0 -> None
                1 -> Requested
                2 -> Accepted
                else -> error("Invalid value for FriendshipState: $value")
            }
        }
    }

}