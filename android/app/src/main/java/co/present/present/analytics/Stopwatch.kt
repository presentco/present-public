package co.present.present.analytics

import org.threeten.bp.Duration
import org.threeten.bp.Instant
import javax.inject.Inject
import javax.inject.Provider


class Stopwatch @Inject constructor(var currentTimeProvider: Provider<Instant>) {

    private var duration = Duration.ZERO
    private var startTime: Instant? = null

    fun start() {
        startTime = currentTimeProvider.get()
    }

    fun stop() {
        startTime?.run {
            duration += timeSince()
            startTime = null
        }
    }

    val time: Duration
    get() {
        startTime?.run {
            return timeSince().plus(duration)
        }
        return duration
    }

    fun Instant.timeSince(): Duration {
        return Duration.between(this, currentTimeProvider.get())
    }
}