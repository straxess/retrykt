package io.github.straxess.retrykt.jitter

import kotlin.time.Duration

/**
 * Changes a backoff delay before RetryKt waits.
 *
 * [Duration.INFINITE] indicates that a finite delay cannot be calculated.
 * RetryKt treats it as a request to stop the retry process.
 */
public fun interface Jitter {

    /**
     * Returns the delay to use for this retry.
     */
    public fun apply(rawDelay: Duration): Duration
}
