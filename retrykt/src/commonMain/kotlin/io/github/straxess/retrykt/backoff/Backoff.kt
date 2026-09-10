package io.github.straxess.retrykt.backoff

import kotlin.time.Duration

/**
 * Calculates the raw delay before the next retry.
 *
 * Implementations must return a finite, non-negative [Duration].
 * RetryKt throws [IllegalStateException] when an implementation violates this contract.
 * Exceptions thrown by an implementation propagate to the caller unchanged.
 */
public interface Backoff {

    /**
     * Returns the raw delay for the retry described by [context].
     */
    public fun nextDelay(context: BackoffContext): Duration
}
