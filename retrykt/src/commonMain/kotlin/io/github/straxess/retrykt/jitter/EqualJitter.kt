package io.github.straxess.retrykt.jitter

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.random.Random
import kotlin.time.Duration

/**
 * Keeps half the backoff delay and randomizes the other half.
 *
 * The result is between `backoffDelay / 2` and [backoffDelay]. Duration rounding can include the upper bound.
 */
public object EqualJitter : Jitter {

    /**
     * @throws IllegalArgumentException if [backoffDelay] is negative or infinite.
     */
    override fun apply(backoffDelay: Duration): Duration = apply(backoffDelay, Random.Default)

    internal fun apply(backoffDelay: Duration, random: Random): Duration {
        requireFiniteNonNegative(backoffDelay, "backoffDelay")

        if (backoffDelay == Duration.ZERO) {
            return Duration.ZERO
        }

        val half = backoffDelay / 2

        return half + half * random.nextDouble()
    }
}
