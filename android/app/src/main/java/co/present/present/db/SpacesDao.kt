package co.present.present.db

import co.present.present.model.Space
import io.reactivex.Flowable

/**
 * These used to get pulled dynamically from the server but now are fixed -- for the first step
 * in refactoring, just left this placeholder here
 */
class SpacesDao {

    fun getSpaces(): Flowable<List<Space>> = Flowable.just(listOf(Space.Everyone, Space.WomenOnly))

}

