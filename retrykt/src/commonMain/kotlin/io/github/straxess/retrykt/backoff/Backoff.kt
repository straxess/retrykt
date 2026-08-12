package io.github.straxess.retrykt.backoff

import kotlin.time.Duration

/**
 * Calculates the delay before the next retry.
 *
 * Return a finite, non-negative [Duration].
 */
public interface Backoff {

    /**
     * Returns the base delay for the retry described by [context].
     */
    public fun nextDelay(context: BackoffContext): Duration
}
