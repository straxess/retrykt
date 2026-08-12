package io.github.straxess.retrykt.backoff

import kotlin.time.Duration

public class LinearBackoff(
    public val increment: Duration,
    public val maxDelay: Duration = Duration.INFINITE,
) : Backoff {

    init {
        require(increment >= Duration.ZERO) {
            "increment must not be negative."
        }

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
