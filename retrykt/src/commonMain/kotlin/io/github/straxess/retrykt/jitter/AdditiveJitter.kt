package io.github.straxess.retrykt.jitter

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.random.Random
import kotlin.time.Duration

/**
 * Adds a random delay in the range `[0, maxJitter)` to the backoff delay.
 *
 * Unlike [FullJitter] and [EqualJitter], this extra delay does not depend on the backoff delay.
 */
public class AdditiveJitter(
    public val maxJitter: Duration,
) : Jitter {

    init {
        requireFiniteNonNegative(maxJitter, "maxJitter")
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
