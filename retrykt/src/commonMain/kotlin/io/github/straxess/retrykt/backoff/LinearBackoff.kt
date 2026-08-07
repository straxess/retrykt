package io.github.straxess.retrykt.backoff

import kotlin.time.Duration

public class LinearBackoff(
    public val increment: Duration,
) : Backoff {

    init {
        require(increment >= Duration.ZERO) {
            "increment must not be negative."
        }
    }

    override fun nextDelay(context: BackoffContext): Duration {
        return increment * context.attempt
    }
}
