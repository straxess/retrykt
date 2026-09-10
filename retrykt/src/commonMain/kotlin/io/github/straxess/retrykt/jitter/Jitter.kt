package io.github.straxess.retrykt.jitter

import kotlin.time.Duration

/**
 * Changes a backoff delay before RetryKt waits.
 *
 * Implementations must return a finite, non-negative [Duration].
 * RetryKt throws [IllegalStateException] when an implementation violates this contract.
 * Exceptions thrown by an implementation propagate to the caller unchanged.
 */
public fun interface Jitter {
    /**
     * Returns the delay to use for this retry.
     */
    public fun apply(rawDelay: Duration): Duration
}
