package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.math.pow
import kotlin.time.Duration

/**
 * Multiplies [initialDelay] by [multiplier] after each attempt, up to the required finite [maxDelay].
 *
 * Requires `0 <= initialDelay <= maxDelay < Duration.INFINITE` and a finite [multiplier] of at least `1.0`.
 *
 * @throws IllegalArgumentException if [initialDelay], [maxDelay], or [multiplier] violates the required bounds.
 */
public class ExponentialBackoff(
    public val initialDelay: Duration,
    public val maxDelay: Duration,
    public val multiplier: Double = 2.0,
) : Backoff {

    init {
        requireFiniteNonNegative(initialDelay, "initialDelay")
        requireFiniteNonNegative(maxDelay, "maxDelay")

        require(multiplier.isFinite()) {
            "multiplier must be finite."
        }

        require(multiplier >= 1.0) {
            "multiplier must not be less than 1.0."
        }

        require(maxDelay >= initialDelay) {
            "maxDelay must not be less than initialDelay."
        }
    }

    override fun nextDelay(context: BackoffContext): Duration {
        val attempt = context.attempt

        if (attempt == 1 || multiplier == 1.0) {
            return initialDelay
        }

        if (initialDelay == Duration.ZERO) {
            return Duration.ZERO
        }

        if (initialDelay == maxDelay) {
            return maxDelay
        }

        val exponent = attempt - 1
        val factor = multiplier.pow(exponent)

        if (factor >= maxDelay / initialDelay) {
            return maxDelay
        }

        return (initialDelay * factor).coerceAtMost(maxDelay)
    }
}
