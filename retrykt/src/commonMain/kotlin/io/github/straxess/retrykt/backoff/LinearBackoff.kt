package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.time.Duration

/**
 * Adds [increment] for each retry, up to [maxDelay].
 */
public class LinearBackoff(
    public val increment: Duration,
    public val maxDelay: Duration = Duration.INFINITE,
) : Backoff {

    init {
        requireFiniteNonNegative(increment, "increment")

        require(maxDelay >= increment) {
            "maxDelay must not be less than increment."
        }
    }

    override fun nextDelay(context: BackoffContext): Duration {
        val delay = increment * context.attempt

        val cappedDelay = delay.coerceAtMost(maxDelay)

        return cappedDelay
    }
}
