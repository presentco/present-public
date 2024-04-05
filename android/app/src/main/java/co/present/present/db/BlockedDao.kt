package co.present.present.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import co.present.present.model.Blocked
import io.reactivex.Flowable

@Dao
abstract class BlockedDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(blocked: List<Blocked>)

    @Query("DELETE FROM Blocked")
    abstract fun clear()

    @Query("SELECT * FROM Blocked")
    abstract fun getBlocked(): Flowable<List<Blocked>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(blocked: Blocked)

    @Query("DELETE FROM Blocked WHERE userId = :userId")
    abstract fun delete(userId: String)
}