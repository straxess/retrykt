package io.github.straxess.retrykt.jitter

import kotlin.time.Duration

/**
 * Applies a constant delay.
 */
public class ConstantJitter(
    private val jitter: Duration
) : Jitter {

    init {
        require(jitter >= Duration.ZERO) {
            "maxJitter must be non-negative."
        }
    }

    override fun apply(baseDelay: Duration): Duration {
        return baseDelay + jitter
    }
}
