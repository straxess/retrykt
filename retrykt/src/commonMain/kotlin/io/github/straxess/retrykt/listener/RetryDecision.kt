package io.github.straxess.retrykt.listener

import kotlin.time.Duration

/**
 * Describes the decision to perform the next retry attempt.
 */
public class RetryDecision internal constructor(

    /**
     * Finite, non-negative delay before the next retry attempt, after jitter.
     */
    public val nextDelay: Duration,
)
