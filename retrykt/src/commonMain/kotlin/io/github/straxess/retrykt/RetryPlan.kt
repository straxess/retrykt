package io.github.straxess.retrykt

import kotlin.time.Duration

/**
 * Describes how the next retry attempt will be scheduled.
 */
public class RetryPlan internal constructor(
    /** Delay after jitter and before the next attempt. */
    public val nextDelay: Duration,
)
