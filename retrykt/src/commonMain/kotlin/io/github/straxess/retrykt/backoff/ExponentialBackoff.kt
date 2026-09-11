package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.math.pow
import kotlin.time.Duration

/**
 * Uses [firstDelay] before the first retry. Each later retry delay is multiplied by [multiplier], without exceeding
 * [maxDelay]. The first attempt starts immediately.
 *
 * Delays must be finite and `0 <= firstDelay <= maxDelay`. [multiplier] must be finite and at least `1.0`.
 *
 * @throws IllegalArgumentException if a value does not meet these requirements.
 */
public class ExponentialBackoff(
    public val firstDelay: Duration,
    public val maxDelay: Duration,
    public val multiplier: Double = 2.0,
) : Backoff {

    init {
        requireFiniteNonNegative(firstDelay, "firstDelay")
        requireFiniteNonNegative(maxDelay, "maxDelay")

        require(multiplier.isFinite()) {
            "multiplier must be finite."
        }

        require(multiplier >= 1.0) {
            "multiplier must not be less than 1.0."
        }

        require(maxDelay >= firstDelay) {
            "maxDelay must not be less than firstDelay."
        }
    }

    override fun calculateDelay(context: BackoffContext): Duration {
        val attempt = context.attempt

        if (attempt == 1 || multiplier == 1.0) {
            return firstDelay
        }

        if (firstDelay == Duration.ZERO) {
            return Duration.ZERO
        }

        if (firstDelay == maxDelay) {
            return maxDelay
        }

        val exponent = attempt - 1
        val factor = multiplier.pow(exponent)

        if (factor >= maxDelay / firstDelay) {
            return maxDelay
        }

        return (firstDelay * factor).coerceAtMost(maxDelay)
    }
}
