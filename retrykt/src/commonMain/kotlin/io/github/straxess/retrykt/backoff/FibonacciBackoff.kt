package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.time.Duration

/**
 * Increases delays according to the Fibonacci sequence.
 *
 * The first two delays are [initialDelay], followed by the sum of the two previous delays, capped at [maxDelay].
 */
public class FibonacciBackoff(
    public val initialDelay: Duration,
    public val maxDelay: Duration = Duration.INFINITE,
) : Backoff {

    init {
        requireFiniteNonNegative(initialDelay, "initialDelay")

        require(maxDelay >= Duration.ZERO) {
            "maxDelay must not be negative."
        }

        require(maxDelay >= initialDelay) {
            "maxDelay must not be less than initialDelay."
        }
    }

    override fun nextDelay(context: BackoffContext): Duration {
        var prevDelay = Duration.ZERO
        var delay = initialDelay

        repeat(context.attempt - 1) {
            if (delay > maxDelay - prevDelay) {
                return maxDelay
            }

            val intermediate = delay
            delay += prevDelay
            prevDelay = intermediate
        }

        val cappedDelay = delay.coerceAtMost(maxDelay)

        return cappedDelay
    }
}
