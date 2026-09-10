package io.github.straxess.retrykt.jitter

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.random.Random
import kotlin.time.Duration

/**
 * Adds a random delay in the range `[0, maxJitter]` to the backoff delay.
 *
 * Unlike [FullJitter] and [EqualJitter], this extra delay does not depend on the backoff delay.
 * The upper bound is reachable because [Duration] arithmetic rounds to representable values.
 * The complete range must be finite;
 * it is rejected instead of being truncated when `rawDelay + maxJitter` overflows to [Duration.INFINITE].
 *
 * @throws IllegalArgumentException if [maxJitter] is negative or infinite.
 */
public class AdditiveJitter(
    public val maxJitter: Duration,
) : Jitter {

    init {
        requireFiniteNonNegative(maxJitter, "maxJitter")
    }

    /**
     * @throws IllegalArgumentException if [rawDelay] is negative or infinite.
     * @throws IllegalStateException if `rawDelay + maxJitter` is infinite.
     */
    override fun apply(rawDelay: Duration): Duration = apply(rawDelay, Random.Default)

    internal fun apply(rawDelay: Duration, random: Random): Duration {
        requireFiniteNonNegative(rawDelay, "rawDelay")

        if (maxJitter == Duration.ZERO) {
            return rawDelay
        }

        val upperBound = rawDelay + maxJitter

        check(upperBound.isFinite()) {
            "rawDelay + maxJitter must be finite."
        }

        return rawDelay + (upperBound - rawDelay) * random.nextDouble()
    }
}
