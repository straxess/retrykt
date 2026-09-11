package io.github.straxess.retrykt.jitter

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.random.Random
import kotlin.time.Duration

/**
 * Returns a random delay from zero to [backoffDelay]. Duration rounding can include the upper bound.
 */
public object FullJitter : Jitter {

    /**
     * @throws IllegalArgumentException if [backoffDelay] is negative or infinite.
     */
    override fun apply(backoffDelay: Duration): Duration = apply(backoffDelay, Random.Default)

    internal fun apply(backoffDelay: Duration, random: Random): Duration {
        requireFiniteNonNegative(backoffDelay, "backoffDelay")

        if (backoffDelay == Duration.ZERO) {
            return Duration.ZERO
        }

        return backoffDelay * random.nextDouble()
    }
}
