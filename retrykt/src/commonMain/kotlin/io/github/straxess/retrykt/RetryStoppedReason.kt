package io.github.straxess.retrykt

/**
 * Describes why RetryKt stopped the retry process.
 */
public sealed interface RetryStoppedReason {

    /**
     * Returns a human-readable description of this stop reason.
     *
     * The returned text is intended for exception messages and diagnostics.
     * Applications should not rely on its exact wording.
     */
    public fun description(): String

    /**
     * Retry stopped because the configured maximum number of attempts has been reached.
     */
    public class MaxAttemptsReached internal constructor(
        public val maxAttempts: Int,
    ) : RetryStoppedReason {
        override fun description(): String =
            "Retry stopped: maximum attempts ($maxAttempts) reached."
    }
}
