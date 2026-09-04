package io.github.straxess.retrykt.backoff

import kotlin.time.Duration

public class BackoffContext internal constructor(

    /**
     * Attempt number, starting at 1.
     */
    public val attempt: Int,

    /**
     * The actual delay applied before the current attempt, after backoff and jitter,
     * or `null` for the first attempt.
     */
    public val lastAppliedDelay: Duration?,
)
