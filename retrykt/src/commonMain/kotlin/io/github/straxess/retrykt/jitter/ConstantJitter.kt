package io.github.straxess.retrykt.jitter

import kotlin.time.Duration

/**
 * Applies a constant jitter.
 */
public class ConstantJitter(
    private val jitter: Duration
) : Jitter {

    init {
        require(jitter >= Duration.ZERO) {
            "jitter must be non-negative."
        }
    }

    override fun apply(baseDelay: Duration): Duration {
        return baseDelay + jitter
    }
}
