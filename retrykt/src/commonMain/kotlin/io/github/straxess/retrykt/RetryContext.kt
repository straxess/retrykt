package io.github.straxess.retrykt

public class RetryContext internal constructor(

    /**
     * Current attempt number. Starts from 1.
     */
    public val attempt: Int,

    /**
     * Maximum allowed attempts.
     */
    public val maxAttempts: Int,

    /**
     * Exception from the previous failed attempt.
     * Null on the first attempt.
     */
    public val lastThrowable: Throwable?
)