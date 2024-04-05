package co.present.present.db

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import co.present.present.extensions.Optional
import co.present.present.model.Circle
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
abstract class CircleDao {

    @get:Query("SELECT * FROM Circle")
    abstract val allCircles: Flowable<List<Circle>>

    @Insert(onConflict = REPLACE)
    abstract fun insert(circle: Circle)

    @Update(onConflict = REPLACE)
    abstract fun update(circle: Circle)

    @Query("DELETE FROM Circle where id = :id")
    abstract fun delete(id: String)

    @Insert(onConflict = REPLACE)
    abstract fun insertCircles(circles: List<Circle>)

    @Query("SELECT * FROM Circle WHERE id = :id")
    abstract fun getCircle(id: String): Flowable<Circle>

    @Query("SELECT * FROM Circle WHERE url = :url")
    abstract fun getCircleByUrl(url: String): Single<Circle>

    @Query("SELECT * FROM Circle WHERE id = :id")
    abstract fun getCircleSync(id: String): Circle

    @Query("SELECT * FROM Circle WHERE id = :id")
    abstract fun getCircles(id: String): Flowable<List<Circle>>

    fun getCircleOptional(id: String): Flowable<Optional<out Circle>> {
        return getCircles(id).map { if (it.isEmpty()) Optional(null) else Optional(it.first()) }
    }

    @Query("SELECT * FROM Circle WHERE id IN (SELECT circleId FROM Nearby)")
    abstract fun getNearbyCircles(): Flowable<List<Circle>>

    @Query("SELECT * FROM Circle WHERE id IN (SELECT circleId FROM Nearby) AND spaceId = :spaceId")
    abstract fun getNearbyCircles(spaceId: String): Flowable<List<Circle>>

    @Query("DELETE FROM Circle WHERE id IN (SELECT circleId FROM Nearby)")
    abstract fun deleteNearbyCircles()

    @Query("SELECT * FROM Circle WHERE joined")
    abstract fun getJoinedCircles(): Flowable<List<Circle>>

    @Query("SELECT * FROM Circle WHERE id IN (:circleIds)")
    abstract fun getJoinedCircles(circleIds: List<String>): Flowable<List<Circle>>

    @Query("SELECT * FROM Circle WHERE unread")
    abstract fun getUnreadCircles(): Flowable<List<Circle>>

    @Query("SELECT * FROM Circle WHERE unread OR (ownerId == :currentUserId AND joinRequests > 0)")
    abstract fun getBadgedCircles(currentUserId: String): Flowable<List<Circle>>

    @Query("SELECT * FROM Circle WHERE joined ORDER BY unread DESC, lastCommentTime DESC")
    abstract fun getJoinedCirclesByLastActivity(): Flowable<List<Circle>>

    @Query("SELECT COUNT(id) FROM Circle WHERE joined")
    abstract fun getJoinedCirclesCount(): Single<Int>

    @Query("DELETE FROM Circle")
    abstract fun clear()

    @Query("DELETE FROM Circle WHERE joined = 0")
    abstract fun clearNonJoined()

    @Query("UPDATE Circle SET unread = :unread  WHERE id = :id")
    abstract fun updateUnread(id: String, unread: Boolean)

    @Transaction
    open fun updateUnread(circles: List<Circle>) {
        circles.forEach { updateUnread(it.id, it.unread) }
    }

    @Transaction
    open fun dropTableAndInsertAll(circles: List<Circle>) {
        clear()
        insertCircles(circles)
    }

    @Transaction
    open fun markRead(circleId: String) {
        val circle = getCircleSync(circleId)
        update(circle.copy(unread = false))
    }
}