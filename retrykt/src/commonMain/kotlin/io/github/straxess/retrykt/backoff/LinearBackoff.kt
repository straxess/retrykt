package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.time.Duration

/**
 * The first retry delay is [increment]. Each later retry adds another [increment], without exceeding [maxDelay].
 *
 * Both durations must be finite and `0 <= increment <= maxDelay`.
 *
 * @throws IllegalArgumentException if a duration is negative or infinite, or [increment] is greater than [maxDelay].
 */
public class LinearBackoff(
    public val increment: Duration,
    public val maxDelay: Duration,
) : Backoff {

    init {
        requireFiniteNonNegative(increment, "increment")
        requireFiniteNonNegative(maxDelay, "maxDelay")

        require(maxDelay >= increment) {
            "maxDelay must not be less than increment."
        }
    }

    override fun calculateDelay(context: BackoffContext): Duration {
        val attempt = context.attempt

        if (increment == Duration.ZERO) {
            return Duration.ZERO
        }

        if (increment == maxDelay) {
            return maxDelay
        }

        if (increment > maxDelay / attempt) {
            return maxDelay
        }

        return increment * attempt
    }
}
