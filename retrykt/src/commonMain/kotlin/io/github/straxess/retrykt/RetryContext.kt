package io.github.straxess.retrykt

/**
 * Information available to the task during an attempt.
 */
public class RetryContext<T> internal constructor(

    /**
     * The current attempt number, starting at 1.
     */
    public val attempt: Int,

    /**
     * The total number of allowed attempts, including the first one.
     */
    public val maxAttempts: Int,

    /**
     * The previous attempt's outcome, or `null` during the first attempt.
     */
    public val prevOutcome: AttemptOutcome<T>?,
)
