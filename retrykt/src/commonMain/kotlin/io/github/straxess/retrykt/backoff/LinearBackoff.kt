package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.time.Duration

/**
 * Adds [increment] for each retry, up to the required finite [maxDelay].
 *
 * Requires `0 <= increment <= maxDelay < Duration.INFINITE`.
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

    override fun nextDelay(context: BackoffContext): Duration {
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
