package io.github.straxess.retrykt.jitter

import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

/**
 * Applies a random delay in the range [0, maxJitter).
 */
public class RandomJitter(
    private val maxJitter: Duration
) : Jitter {

    init {
        require(maxJitter >= Duration.ZERO) {
            "maxJitter must be non-negative."
        }
    }

    override fun apply(baseDelay: Duration): Duration {
        return baseDelay + generateRandomJitter()
    }

    private fun generateRandomJitter(): Duration {
        return if (maxJitter == Duration.ZERO) {
            Duration.ZERO
        } else {
            Random.nextLong(maxJitter.inWholeMilliseconds).microseconds
        }
    }
}
