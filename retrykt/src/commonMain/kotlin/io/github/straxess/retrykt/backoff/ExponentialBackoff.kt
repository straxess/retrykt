package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.jitter.Jitter
import io.github.straxess.retrykt.jitter.NoJitter
import kotlin.math.pow
import kotlin.time.Duration

public class ExponentialBackoff(
    private val delay: Duration,
    private val stepMultiplier: Double,
    private val jitter: Jitter = NoJitter,
) : Backoff {

    init {
        require(delay >= Duration.ZERO) {
            "baseDelay must not be negative."
        }

        require(stepMultiplier > 0) {
            "stepMultiplier must be positive."
        }
    }

    override fun nextDelay(attempt: Int): Duration {
        stepMultiplier.pow(attempt)
        val baseDelay = delay * stepMultiplier.pow(attempt)
        return jitter.apply(baseDelay)
    }
}
