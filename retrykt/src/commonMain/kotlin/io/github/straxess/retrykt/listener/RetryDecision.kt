package io.github.straxess.retrykt.listener

import kotlin.time.Duration

/**
 * Describes the decision to perform the next retry attempt.
 */
public class RetryDecision internal constructor(
    /**
     * Delay before the next retry attempt.
     */
    public val nextDelay: Duration,
)
