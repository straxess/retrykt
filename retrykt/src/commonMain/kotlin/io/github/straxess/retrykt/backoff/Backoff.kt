package io.github.straxess.retrykt.backoff

import kotlin.time.Duration

/**
 * Calculates the delay before the next retry.
 *
 * [Duration.INFINITE] indicates that a finite delay cannot be calculated.
 * RetryKt treats it as a request to stop the retry process.
 */
public interface Backoff {

    /**
     * Returns the base delay for the retry described by [context].
     */
    public fun nextDelay(context: BackoffContext): Duration
}
