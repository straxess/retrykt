package io.github.straxess.retrykt.internal

import kotlin.time.Duration

/**
 * Blocks the current thread for [duration], or returns immediately when it is zero.
 *
 * @throws IllegalArgumentException if [duration] is negative or infinite.
 */
internal fun sleep(duration: Duration) {
    require(!duration.isNegative())
    require(duration.isFinite())

    if (duration == Duration.ZERO) {
        return
    }

    sleepInternal(duration)
}

/**
 * Blocks the current thread for a finite, positive [duration].
 */
internal expect fun sleepInternal(duration: Duration)
