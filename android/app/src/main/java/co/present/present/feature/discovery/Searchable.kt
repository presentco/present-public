package co.present.present.feature.discovery

import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable

interface Searchable {
    fun getSearchTerm(): Flowable<String>
    fun searchChanged(searchTerm: String)
}

class SearchableImpl : Searchable {

    private val searchTermSubject = BehaviorRelay.createDefault("")

    override fun searchChanged(searchTerm: String) {
        searchTermSubject.accept(searchTerm)
    }

    override fun getSearchTerm(): Flowable<String> {
        return searchTermSubject.toFlowable(BackpressureStrategy.LATEST)
    }

}