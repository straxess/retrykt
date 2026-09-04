package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.math.pow
import kotlin.time.Duration

/**
 * Multiplies [initialDelay] by [multiplier] after each attempt, up to [maxDelay].
 */
public class ExponentialBackoff(
    public val initialDelay: Duration,
    public val multiplier: Double = 2.0,
    public val maxDelay: Duration = Duration.INFINITE,
) : Backoff {

    init {
        requireFiniteNonNegative(initialDelay, "initialDelay")

        require(maxDelay >= Duration.ZERO) {
            "maxDelay must not be negative."
        }

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
        val delay = initialDelay * multiplier.pow(context.attempt - 1)

        val cappedDelay = delay.coerceAtMost(maxDelay)

        return cappedDelay
    }
}
