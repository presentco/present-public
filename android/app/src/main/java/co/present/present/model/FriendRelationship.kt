package co.present.present.model

import androidx.room.Entity

@Entity(primaryKeys = arrayOf("userId", "otherUserId"))
class FriendRelationship(val userId: String, val otherUserId: String, val state: Int) {

    val friendshipState get() = FriendshipState.fromValue(state)
    
}