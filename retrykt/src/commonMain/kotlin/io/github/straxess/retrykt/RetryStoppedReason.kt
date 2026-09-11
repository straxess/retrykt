package io.github.straxess.retrykt

/**
 * The reason why RetryKt stopped retrying.
 */
public sealed interface RetryStoppedReason {

    /**
     * Returns a description for logs or error messages.
     *
     * Do not parse or compare the returned text because it may change.
     */
    public fun description(): String

    /**
     * The operation used all [maxAttempts] attempts.
     */
    public class MaxAttemptsReached internal constructor(
        public val maxAttempts: Int,
    ) : RetryStoppedReason {
        override fun description(): String = "Retry stopped: maximum attempts ($maxAttempts) reached."
    }
}
