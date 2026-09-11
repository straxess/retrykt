package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.time.Duration

/**
 * Returns the same [delay] before every retry.
 *
 * @throws IllegalArgumentException if [delay] is negative or infinite.
 */
public class ConstantBackoff(
    public val delay: Duration,
) : Backoff {

    init {
        requireFiniteNonNegative(delay, "delay")
    }

    override fun calculateDelay(context: BackoffContext): Duration = delay
}
