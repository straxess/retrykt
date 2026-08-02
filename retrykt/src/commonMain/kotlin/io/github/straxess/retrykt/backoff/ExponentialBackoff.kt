package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.jitter.Jitter
import io.github.straxess.retrykt.jitter.NoJitter
import kotlin.math.pow
import kotlin.time.Duration

public class ExponentialBackoff(
    public val initialDelay: Duration,
    public val multiplier: Double = 2.0,
    public val maxDelay: Duration = Duration.INFINITE,
    public val jitter: Jitter = NoJitter,
) : Backoff {

    init {
        require(initialDelay >= Duration.ZERO) {
            "baseDelay must not be negative."
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

    override fun nextDelay(attempt: Int): Duration {
        val baseDelay = initialDelay * multiplier.pow(attempt - 1)

        val cappedDelay = baseDelay.coerceAtMost(maxDelay)

        return jitter.apply(cappedDelay)
    }
}
