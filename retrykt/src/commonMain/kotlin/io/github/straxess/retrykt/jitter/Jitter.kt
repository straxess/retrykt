package io.github.straxess.retrykt.jitter

import kotlin.time.Duration

/**
 * Changes a backoff delay before the next attempt.
 *
 * An implementation must return a finite, non-negative [Duration]. RetryKt throws [IllegalStateException] for an
 * invalid result. An exception from the implementation is passed to the caller unchanged.
 */
public fun interface Jitter {
    /**
     * Returns the next applied delay based on [backoffDelay].
     */
    public fun apply(backoffDelay: Duration): Duration
}
