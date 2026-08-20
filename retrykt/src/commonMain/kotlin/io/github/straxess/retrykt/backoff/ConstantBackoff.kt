package io.github.straxess.retrykt.backoff

import kotlin.time.Duration

/**
 * Uses the same [delay] before every retry.
 */
public class ConstantBackoff(
    public val delay: Duration,
) : Backoff {

    init {
        require(delay >= Duration.ZERO) {
            "delay must not be negative."
        }
    }

    override fun nextDelay(context: BackoffContext): Duration = delay
}
