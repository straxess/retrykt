package io.github.straxess.retrykt.backoff

import kotlin.time.Duration

/** Information used to calculate the next delay. */
public class BackoffContext internal constructor(

    /**
     * The number of the completed attempt, starting at 1.
     */
    public val attempt: Int,

    /**
     * The applied delay passed to the waiting function before this attempt, or `null` after the first attempt.
     *
     * This is the requested delay, not the measured waiting time.
     */
    public val prevAppliedDelay: Duration?,
)
