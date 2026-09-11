package io.github.straxess.retrykt

/**
 * The reason why RetryKt exhausted the retry process.
 */
public sealed interface RetryExhaustionReason {

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
    ) : RetryExhaustionReason {
        override fun description(): String = "Retry exhausted: maximum attempts ($maxAttempts) reached."
    }
}
