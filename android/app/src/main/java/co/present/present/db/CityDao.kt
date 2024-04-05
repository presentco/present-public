package co.present.present.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import co.present.present.model.City
import io.reactivex.Flowable

@Dao
abstract class CityDao {

    @Query("SELECT * FROM City")
    abstract fun getCities(): Flowable<List<City>>

    @Query("DELETE FROM City")
    abstract fun clear()

    @Insert(onConflict = REPLACE)
    abstract fun insertCircles(cities: List<City>)

    @Transaction
    open fun dropTableAndInsertAll(cities: List<City>) {
        clear()
        insertCircles(cities)
    }

}

