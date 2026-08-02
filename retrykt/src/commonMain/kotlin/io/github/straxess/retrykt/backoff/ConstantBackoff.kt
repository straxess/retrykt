package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.jitter.Jitter
import io.github.straxess.retrykt.jitter.NoJitter
import kotlin.time.Duration

public class ConstantBackoff(
    public val delay: Duration,
    public val jitter: Jitter = NoJitter,
) : Backoff {

    init {
        require(delay >= Duration.ZERO) {
            "delay must not be negative."
        }
    }

    override fun nextDelay(attempt: Int): Duration {
        return jitter.apply(delay)
    }
}
