package co.present.present.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import co.present.present.model.Nearby

@Dao
abstract class NearbyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(nearby: List<Nearby>)

    @Query("DELETE FROM Nearby")
    abstract fun clear()
}