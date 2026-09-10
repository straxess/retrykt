package io.github.straxess.retrykt.jitter

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.random.Random
import kotlin.time.Duration

/**
 * AWS-style full jitter: returns a random delay in the range `[0, rawDelay]`.
 * The upper bound is reachable because [Duration] arithmetic rounds to representable values.
 */
public object FullJitter : Jitter {

    /**
     * @throws IllegalArgumentException if [rawDelay] is negative or infinite.
     */
    override fun apply(rawDelay: Duration): Duration = apply(rawDelay, Random.Default)

    internal fun apply(rawDelay: Duration, random: Random): Duration {
        requireFiniteNonNegative(rawDelay, "rawDelay")

        if (rawDelay == Duration.ZERO) {
            return Duration.ZERO
        }

        return rawDelay * random.nextDouble()
    }
}
