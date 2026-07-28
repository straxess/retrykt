package llemur.retry.backoff

import llemur.retry.jitter.Jitter
import llemur.retry.jitter.NoJitter
import kotlin.time.Duration

class ConstantBackoff(
    private val delay: Duration,
    private val jitter: Jitter = NoJitter,
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
