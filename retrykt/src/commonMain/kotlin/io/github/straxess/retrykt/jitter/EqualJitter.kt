package io.github.straxess.retrykt.jitter

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.random.Random
import kotlin.time.Duration

/**
 * AWS-style equal jitter: keeps half the delay and randomizes the other half.
 * The result is in the range `[rawDelay / 2, rawDelay]`;
 * the upper bound is reachable because [Duration] arithmetic rounds to representable values.
 */
public object EqualJitter : Jitter {

    /**
     * @throws IllegalArgumentException if [rawDelay] is negative or infinite.
     */
    override fun apply(rawDelay: Duration): Duration = apply(rawDelay, Random.Default)

    internal fun apply(rawDelay: Duration, random: Random): Duration {
        requireFiniteNonNegative(rawDelay, "rawDelay")

        if (rawDelay == Duration.ZERO) {
            return Duration.ZERO
        }

        val half = rawDelay / 2

        return half + half * random.nextDouble()
    }
}
