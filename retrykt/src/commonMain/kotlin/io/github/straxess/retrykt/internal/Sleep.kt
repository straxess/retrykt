package io.github.straxess.retrykt.internal

import kotlin.time.Duration

/**
 * Blocks the current thread for [duration].
 *
 * Does nothing when [duration] is [Duration.ZERO].
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
 * Blocks the current thread for [duration].
 *
 * The [duration] must be finite and greater than [Duration.ZERO].
 */
internal expect fun sleepInternal(duration: Duration)
