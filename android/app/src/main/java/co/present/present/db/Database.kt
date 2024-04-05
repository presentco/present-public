package co.present.present.db


import android.util.Log
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import co.present.present.extensions.applyCompletableSchedulers
import co.present.present.model.*
import io.reactivex.Completable
import io.reactivex.rxkotlin.subscribeBy
import present.proto.UserResponse


@Database(entities =
[
    CurrentUser::class,
    FriendRelationship::class,
    User::class,
    Circle::class,
    City::class,
    Nearby::class,
    Blocked::class
],
        version = 2)
@TypeConverters(Converter::class)
abstract class Database : RoomDatabase() {
    val TAG: String = javaClass.simpleName
    abstract fun userDao(): UserDao
    abstract fun currentUserDao(): CurrentUserDao
    abstract fun friendRelationshipDao(): FriendRelationshipDao
    abstract fun circleDao(): CircleDao
    abstract fun cityDao(): CityDao
    abstract fun nearbyDao(): NearbyDao
    abstract fun blockedDao(): BlockedDao

    fun persistUserProfileAsync(profile: present.proto.UserProfile?) {
        profile?.let { persistUserProfile(it).compose(applyCompletableSchedulers()).subscribeBy(
                onError = {
                    Log.d(TAG, "Error persisting user profile to database")
                },
                onComplete = {}
        ) }
    }

    fun persistUserProfile(profile: present.proto.UserProfile?): Completable {
        return Completable.fromCallable {
            profile?.let {
                runInTransaction {
                    val currentUser = CurrentUser(it)
                    currentUserDao().insert(currentUser)
                    userDao().insert(User(currentUser))
                }
            }
        }.doOnError { e ->
                    Log.e(TAG, "Error saving current user to database", e)
                }.doOnComplete {
                    Log.d(TAG, "Current user saved to database")
                }
    }

    fun persistUser(userResponse: UserResponse): Completable {
        return Completable.fromCallable {
            runInTransaction {
                User(userResponse).also {
                    userDao().insert(it)
                }
            }
        }.doOnError { e ->
                    Log.e(TAG, "Error saving user ${userResponse.name} to database", e)
                }.doOnComplete {
                    Log.d(TAG, "User ${userResponse.name} saved to database")
                }
    }

    fun persistNearby(nearbyCircles: List<Circle>): Completable {
        return Completable.fromCallable {
            runInTransaction {
                circleDao().insertCircles(nearbyCircles)
                nearbyDao().clear()
                nearbyDao().insertAll(nearbyCircles.map { Nearby(circleId = it.id) })
            }
        }.doOnError { e ->
                    Log.e(TAG, "Error saving ${nearbyCircles.size} nearby circles to database", e)
                }.doOnComplete {
                    Log.d(TAG, "${nearbyCircles.size} nearby circles saved to database")
                }
    }

    fun persistFriends(userId: String, friends: List<User>): Completable {
        return Completable.fromCallable {
            runInTransaction {
                friends.forEach { friend ->
                    userDao().insertOrPartialUpdate(friend)
                    friendRelationshipDao().addFriendship(userId, friend.id)
                }
                Log.d(TAG, "Inserted ${friends.size} friends into the database")
            }
        }
    }
}
