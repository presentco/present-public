package co.present.present.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import co.present.present.model.User
import io.reactivex.Flowable

@Dao
abstract class UserDao {
    @get:Query("SELECT * FROM User")
    abstract val users: Flowable<List<User>>

    @Query("SELECT * FROM User WHERE id = :id")
    abstract fun getUser(id: String): Flowable<User>

    @Query("SELECT * FROM User WHERE id = :id")
    abstract fun getUserSync(id: String): User

    @Query("SELECT * FROM User WHERE id = :id")
    abstract fun getUserList(id: String): Flowable<List<User>>

    @Insert(onConflict = REPLACE)
    abstract fun insert(user: User)

    @Insert(onConflict = REPLACE)
    abstract fun insert(users: List<User>)

    @Query("DELETE FROM User")
    abstract fun deleteAll()

    @Query("SELECT User.* FROM User, CurrentUser WHERE User.id = CurrentUser.id")
    abstract fun getCurrentUser(): Flowable<User>

    @Query("SELECT User.* FROM User, CurrentUser WHERE User.id = CurrentUser.id")
    abstract fun getCurrentUserSync(): User

    @Query("SELECT User.* FROM User, FriendRelationship WHERE state = 2 AND FriendRelationship.otherUserId = User.id AND FriendRelationship.userId = :userId")
    abstract fun getFriends(userId: String): Flowable<List<User>>

    /**
     * This class is needed because not all User objects come with all fields populated.
     * For example, the users in UserResponse.friends and CurrentUser.friends lack interests.
     *
     * @param user
     */
    @Transaction
    open fun insertOrPartialUpdate(user: User) {
        val oldUser: User? = getUserSync(user.id)
        if (oldUser != null) {
            // Overwrite just the fields we guarantee are populated.
            val newUser = oldUser.copy(
                    bio = user.bio,
                    firstName = user.firstName,
                    name = user.name,
                    photo = user.photo)
            insert(newUser)
        } else {
            // This is only a partially populated copy of the User object, but it's better than
            // nothing and will suffice for displaying the user preview.  If we need more, that
            // screen will try and fetch the full object
            insert(user)
        }
    }

}