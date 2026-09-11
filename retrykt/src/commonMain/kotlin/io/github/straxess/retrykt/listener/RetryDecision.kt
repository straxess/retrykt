package io.github.straxess.retrykt.listener

import kotlin.time.Duration

/**
 * Information about the next retry.
 */
public class RetryDecision internal constructor(

    /**
     * The finite, non-negative delay that will be passed to the waiting function before the next attempt.
     */
    public val nextAppliedDelay: Duration,
)
