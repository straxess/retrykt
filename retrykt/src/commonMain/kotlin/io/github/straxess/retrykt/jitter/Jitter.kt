package io.github.straxess.retrykt.jitter

import kotlin.time.Duration

/**
 * Changes a backoff delay before RetryKt waits.
 *
 * Return a finite, non-negative [Duration].
 */
public fun interface Jitter {

    /**
     * Returns the delay to use for this retry.
     */
    public fun apply(rawDelay: Duration): Duration
}
