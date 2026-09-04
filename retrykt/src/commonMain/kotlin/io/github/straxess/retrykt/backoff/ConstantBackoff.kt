package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.time.Duration

/**
 * Uses the same [delay] before every retry.
 */
public class ConstantBackoff(
    public val delay: Duration,
) : Backoff {

    init {
        requireFiniteNonNegative(delay, "delay")
    }

    override fun nextDelay(context: BackoffContext): Duration = delay
}
