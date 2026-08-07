package io.github.straxess.retrykt.backoff

import kotlin.time.Duration

public class BackoffContext internal constructor(

    /**
     * The current attempt number, starting from 1.
     */
    public val attempt: Int,

    /**
     * The delay applied before the previous attempt.
     *
     * This value is `null` for the first attempt.
     */
    public val lastAppliedDelay: Duration?,
)
