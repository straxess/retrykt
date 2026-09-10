package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.time.Duration

/**
 * Increases delays according to the Fibonacci sequence.
 *
 * The first two delays are [initialDelay], followed by the sum of the two previous delays, capped at [maxDelay].
 * When [initialDelay] is zero, every delay is zero regardless of [maxDelay].
 * Requires `0 <= initialDelay <= maxDelay < Duration.INFINITE`.
 *
 * @throws IllegalArgumentException if [initialDelay] or [maxDelay] violates the required bounds.
 */
public class FibonacciBackoff(
    public val initialDelay: Duration,
    public val maxDelay: Duration,
) : Backoff {

    init {
        requireFiniteNonNegative(initialDelay, "initialDelay")
        requireFiniteNonNegative(maxDelay, "maxDelay")

        require(maxDelay >= initialDelay) {
            "maxDelay must not be less than initialDelay."
        }
    }

    override fun nextDelay(context: BackoffContext): Duration {
        if (initialDelay == Duration.ZERO) {
            return Duration.ZERO
        }

        var prevDelay = Duration.ZERO
        var delay = initialDelay

        repeat(context.attempt - 1) {
            if (delay >= maxDelay - prevDelay) {
                return maxDelay
            }

            val intermediate = delay
            delay += prevDelay
            prevDelay = intermediate
        }

        return delay
    }
}
