package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.math.log
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
        val attempt = context.attempt

        if (attempt <= 1) {
            return initialDelay
        }

        if (initialDelay == Duration.ZERO) {
            return Duration.ZERO
        }

        if (initialDelay == maxDelay) {
            return maxDelay
        }

        if (!maxDelay.isFinite()) {
            val power = multiplier.pow(attempt - 1)

            if (!power.isFinite()) {
                return Duration.INFINITE
            }

            return initialDelay * power
        }

        val exponent = attempt - 1
        val maxExponent = log(maxDelay / initialDelay, multiplier)

        if (exponent.toDouble() >= maxExponent) {
            return maxDelay
        }

        return initialDelay * multiplier.pow(exponent)
    }
}
