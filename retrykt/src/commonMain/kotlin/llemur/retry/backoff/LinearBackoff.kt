package llemur.retry.backoff

import llemur.retry.jitter.Jitter
import llemur.retry.jitter.NoJitter
import kotlin.time.Duration

class LinearBackoff(
    private val step: Duration,
    private val jitter: Jitter = NoJitter,
) : Backoff {

    init {
        require(step >= Duration.ZERO) {
            "step must not be negative."
        }
    }

    override fun nextDelay(attempt: Int): Duration {
        val baseDelay = step * attempt
        return jitter.apply(baseDelay)
    }
}
