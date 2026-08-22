package io.github.straxess.retrykt.backoff

import kotlin.time.Duration

public class FibonacciBackoff(
    public val initialDelay: Duration,
    public val maxDelay: Duration = Duration.INFINITE,
) : Backoff {

    init {
        require(initialDelay >= Duration.ZERO) {
            "initialDelay must not be negative."
        }

        require(maxDelay >= Duration.ZERO) {
            "maxDelay must not be negative."
        }

        require(maxDelay >= initialDelay) {
            "maxDelay must not be less than initialDelay."
        }
    }

    override fun nextDelay(context: BackoffContext): Duration {
        var prevDelay = Duration.ZERO
        var delay = initialDelay

        repeat(context.attempt - 1) {
            if (delay > maxDelay - prevDelay) {
                return maxDelay
            }

            val intermediate = delay
            delay += prevDelay
            prevDelay = intermediate
        }

        val cappedDelay = delay.coerceAtMost(maxDelay)

        return cappedDelay
    }
}
