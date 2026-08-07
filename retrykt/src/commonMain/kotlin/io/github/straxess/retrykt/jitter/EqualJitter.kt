package io.github.straxess.retrykt.jitter

import kotlin.random.Random
import kotlin.time.Duration

/**
 * Applies the Equal Jitter strategy recommended by AWS.
 *
 * Preserves at least half of the computed backoff delay
 * and adds a uniformly distributed random delay from the remaining half,
 * producing a delay in the range [rawDelay / 2, rawDelay).
 */
public object EqualJitter : Jitter {

    override fun apply(rawDelay: Duration): Duration {
        require(rawDelay >= Duration.ZERO) {
            "rawDelay must be non-negative."
        }

        if (rawDelay == Duration.ZERO) {
            return Duration.ZERO
        }

        val half = rawDelay / 2

        return half + half * Random.nextDouble()
    }
}
