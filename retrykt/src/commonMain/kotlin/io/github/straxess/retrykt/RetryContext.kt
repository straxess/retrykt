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
)
