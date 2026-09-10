package io.github.straxess.retrykt.backoff

import kotlin.time.Duration

public class BackoffContext internal constructor(

    /**
     * Number of the completed attempt that caused this backoff calculation, starting at 1.
     */
    public val attempt: Int,

    /**
     * The previous actual applied delay, after backoff and jitter, or `null` for the first attempt.
     */
    public val prevAppliedDelay: Duration?,
)
