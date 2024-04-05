package co.present.present.db

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import co.present.present.model.FriendRelationship
import co.present.present.model.FriendshipState
import co.present.present.model.User
import io.reactivex.Flowable

@Dao
abstract class FriendRelationshipDao {

    @Insert(onConflict = REPLACE)
    abstract fun insert(friendRelationship: FriendRelationship)

    @Insert(onConflict = REPLACE)
    abstract fun insert(friendRelationships: List<FriendRelationship>)

    @Query("DELETE FROM FriendRelationship WHERE userId = :userId")
    abstract fun deleteOutboundRequests(userId: String)


    @Query("DELETE FROM FriendRelationship WHERE otherUserId = :userId")
    abstract fun deleteInboundRequests(userId: String)

    /**
     * People this user has requested
     */
    @Query("SELECT * FROM User WHERE id IN (SELECT otherUserId FROM FriendRelationship WHERE userId = :userId AND state = 1)")
    abstract fun getOutboundRequestedUsers(userId: String): Flowable<List<User>>

    /**
     * People this user is requested by
     */
    @Query("SELECT * FROM User WHERE id IN (SELECT userId FROM FriendRelationship WHERE otherUserId = :userId AND state = 1)")
    abstract fun getInboundRequestedUsers(userId: String): Flowable<List<User>>

    /**
     * If userId has requested otherId
     */
    @Query("SELECT * from FriendRelationship WHERE userId = :userId AND otherUserId = :otherUserId AND state = 1")
    abstract fun getFriendRequest(userId: String, otherUserId: String): Flowable<List<FriendRelationship>>

    fun isRequested(userId: String, otherUserId: String): Flowable<Boolean> {
        return getFriendRequest(userId, otherUserId).map { !it.isEmpty() }
    }

    fun getFriendshipState(userId: String, otherUserId: String): Flowable<FriendshipState> {
        return getFriendshipStateInt(userId, otherUserId).map { if (it.isEmpty()) FriendshipState.None else FriendshipState.fromValue(it.first()) }
    }

    @Query("SELECT state FROM FriendRelationship WHERE userId = :userId AND otherUserId = :otherUserId")
    abstract fun getFriendshipStateInt(userId: String, otherUserId: String): Flowable<List<Int>>

    /**
     * Add a symmetric accepted friendship between two users
     */
    @Transaction
    open fun addFriendship(userId: String, otherUserId: String) {
        insert(FriendRelationship(userId, otherUserId, FriendshipState.Accepted.value))
        insert(FriendRelationship(otherUserId, userId, FriendshipState.Accepted.value))
    }

    /**
     * Completely remove the friendship between two users
     */
    @Transaction
    open fun deleteFriendship(userId: String, otherUserId: String) {
        delete(FriendRelationship(userId, otherUserId, FriendshipState.Accepted.value))
        delete(FriendRelationship(otherUserId, userId, FriendshipState.Accepted.value))
        delete(FriendRelationship(userId, otherUserId, FriendshipState.Requested.value))
        delete(FriendRelationship(otherUserId, userId, FriendshipState.Requested.value))
    }

    @Delete
    abstract fun delete(friendRelationship: FriendRelationship)

    @Query("SELECT * FROM FriendRelationship WHERE state = 2 AND (otherUserId = :otherUserId AND userId = :userId) OR (otherUserId = :userId AND userId = :otherUserId)")
    abstract fun getFriendships(userId: String, otherUserId: String): Flowable<List<FriendRelationship>>

    /**
     * All outgoing relationships (friendships and requests)
     */
    @Query("SELECT * FROM FriendRelationship WHERE userId = :userId")
    abstract fun getRelationships(userId: String): Flowable<List<FriendRelationship>>

    @Query("SELECT count(*) FROM FriendRelationship WHERE otherUserId = :userId AND state = 1")
    abstract fun getInboundRequestCount(userId: String): Flowable<Int>

}