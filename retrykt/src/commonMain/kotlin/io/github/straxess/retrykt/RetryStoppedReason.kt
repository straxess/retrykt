package io.github.straxess.retrykt

/**
 * Describes why RetryKt stopped the retry process.
 */
public sealed interface RetryStoppedReason {

    /**
     * Returns a human-readable description of this stop reason.
     *
     * Applications should not rely on its exact wording.
     */
    public fun description(): String

    /**
     * Retry stopped because the configured maximum number of attempts has been reached.
     */
    public class MaxAttemptsReached internal constructor(
        public val maxAttempts: Int,
    ) : RetryStoppedReason {
        override fun description(): String = "Retry stopped: maximum attempts ($maxAttempts) reached."
    }

    /**
     * Retry stopped because the resulting delay is infinite.
     *
     * This can occur when a backoff or jitter produces [kotlin.time.Duration.INFINITE].
     */
    public class InfiniteDelay internal constructor() : RetryStoppedReason {
        override fun description(): String = "Retry stopped: infinite delay occurred."
    }
}
