package io.github.straxess.retrykt

public class RetryContext<T> internal constructor(

    /**
     * Current attempt number, starting at 1.
     */
    public val attempt: Int,

    /**
     * Maximum total task invocations, including the initial attempt.
     */
    public val maxAttempts: Int,

    /**
     * Outcome of the previous attempt, or `null` for the first attempt.
     */
    public val prevOutcome: AttemptOutcome<T>?,
)
