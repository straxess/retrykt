package io.github.straxess.retrykt.jitter

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.random.Random
import kotlin.time.Duration

/**
 * AWS-style equal jitter: keeps half the delay and randomizes the other half.
 * The result is in the range `[rawDelay / 2, rawDelay)`.
 */
public object EqualJitter : Jitter {

    override fun apply(rawDelay: Duration): Duration {
        requireFiniteNonNegative(rawDelay, "rawDelay")

        if (rawDelay == Duration.ZERO) {
            return Duration.ZERO
        }

        val half = rawDelay / 2

        return half + half * Random.nextDouble()
    }
}
