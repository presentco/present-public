package co.present.present.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import co.present.present.extensions.Optional
import co.present.present.model.CurrentUser
import io.reactivex.Flowable
import io.reactivex.Maybe

@Dao
abstract class CurrentUserDao {

    @get:Query("SELECT * FROM CurrentUser")
    internal abstract val asFlowable: Flowable<CurrentUser>

    @get:Query("SELECT * FROM CurrentUser")
    internal abstract val list: Flowable<List<CurrentUser>>

    internal val optional: Flowable<Optional<CurrentUser>> get() {
        return list.map { if (it.isEmpty()) Optional<CurrentUser>(null) else Optional(it.first()) }
    }

    @Query("SELECT * FROM CurrentUser")
    internal abstract fun get(): Maybe<CurrentUser>

    @Insert(onConflict = REPLACE)
    internal abstract fun insert(currentUser: CurrentUser)

    @Query("DELETE FROM CurrentUser")
    internal abstract fun clear()
}