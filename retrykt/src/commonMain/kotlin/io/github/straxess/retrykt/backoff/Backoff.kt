package io.github.straxess.retrykt.backoff

import kotlin.time.Duration

/**
 * Calculates the backoff delay before the next attempt.
 *
 * An implementation must return a finite, non-negative [Duration]. RetryKt throws [IllegalStateException] for an
 * invalid result. An exception from the implementation is passed to the caller unchanged.
 */
public interface Backoff {

    /**
     * Returns the backoff delay for the retry described by [context].
     */
    public fun calculateDelay(context: BackoffContext): Duration
}
