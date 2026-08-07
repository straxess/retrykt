package io.github.straxess.retrykt.jitter

import kotlin.random.Random
import kotlin.time.Duration

/**
 * Adds a uniformly distributed random delay in the range [0, maxJitter) to the computed backoff delay.
 *
 * Unlike [FullJitter] and [EqualJitter], the jitter amount is independent of the computed backoff delay.
 */
public class AdditiveJitter(
    public val maxJitter: Duration,
) : Jitter {

    init {
        require(maxJitter >= Duration.ZERO) {
            "maxJitter must be non-negative."
        }

        require(maxJitter.isFinite()) {
            "maxJitter must be finite."
        }
    }

    override fun apply(rawDelay: Duration): Duration {
        require(rawDelay >= Duration.ZERO) {
            "rawDelay must be non-negative."
        }

        if (maxJitter == Duration.ZERO) {
            return rawDelay
        }

        return rawDelay + maxJitter * Random.nextDouble()
    }
}
