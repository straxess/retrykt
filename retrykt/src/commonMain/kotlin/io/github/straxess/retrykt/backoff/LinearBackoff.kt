package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.jitter.Jitter
import io.github.straxess.retrykt.jitter.NoJitter
import kotlin.time.Duration

public class LinearBackoff(
    public val increment: Duration,
    public val jitter: Jitter = NoJitter,
) : Backoff {

    init {
        require(increment >= Duration.ZERO) {
            "increment must not be negative."
        }
    }

    override fun nextDelay(attempt: Int): Duration {
        val baseDelay = increment * (attempt)
        return jitter.apply(baseDelay)
    }
}
