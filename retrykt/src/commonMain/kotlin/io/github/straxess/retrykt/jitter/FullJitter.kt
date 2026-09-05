package io.github.straxess.retrykt.jitter

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.random.Random
import kotlin.time.Duration

/**
 * AWS-style full jitter: returns a random delay in the range `[0, rawDelay)`.
 */
public object FullJitter : Jitter {

    override fun apply(rawDelay: Duration): Duration {
        requireFiniteNonNegative(rawDelay, "rawDelay")

        if (rawDelay == Duration.ZERO) {
            return Duration.ZERO
        }

        return rawDelay * Random.nextDouble()
    }
}
