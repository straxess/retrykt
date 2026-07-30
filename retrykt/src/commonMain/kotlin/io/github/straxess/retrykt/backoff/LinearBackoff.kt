package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.jitter.Jitter
import io.github.straxess.retrykt.jitter.NoJitter
import kotlin.time.Duration

public class LinearBackoff(
    private val step: Duration,
    private val jitter: Jitter = NoJitter,
) : Backoff {

    init {
        require(step >= Duration.ZERO) {
            "step must not be negative."
        }
    }

    override fun nextDelay(attempt: Int): Duration {
        val baseDelay = step * (attempt + 1)
        return jitter.apply(baseDelay)
    }
}
