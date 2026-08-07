package io.github.straxess.retrykt.backoff

import kotlin.math.pow
import kotlin.time.Duration

public class ExponentialBackoff(
    public val initialDelay: Duration,
    public val multiplier: Double = 2.0,
    public val maxDelay: Duration = Duration.INFINITE,
) : Backoff {

    init {
        require(initialDelay >= Duration.ZERO) {
            "initialDelay must not be negative."
        }

        require(maxDelay >= Duration.ZERO) {
            "maxDelay must not be negative."
        }

        require(multiplier.isFinite()) {
            "multiplier must be finite."
        }

        require(multiplier >= 1.0) {
            "multiplier must not be less than 1.0."
        }
    }

    override fun nextDelay(context: BackoffContext): Duration {
        val delay = initialDelay * multiplier.pow(context.attempt - 1)

        val cappedDelay = delay.coerceAtMost(maxDelay)

        return cappedDelay
    }
}
