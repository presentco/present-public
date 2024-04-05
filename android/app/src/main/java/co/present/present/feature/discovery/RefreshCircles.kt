package co.present.present.feature.discovery

import io.reactivex.Completable

interface RefreshCircles {

    fun clearAndRefreshCircles(): Completable

    fun refreshCircles(): Completable

    fun clearAndRefreshNearbyCircles(): Completable

    fun refreshNearby(): Completable

    fun refreshJoined(): Completable
}