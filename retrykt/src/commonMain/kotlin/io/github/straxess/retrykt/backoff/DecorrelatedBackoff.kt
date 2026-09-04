package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.random.Random
import kotlin.time.Duration

/**
 * Randomized backoff based on the AWS decorrelated-jitter algorithm.
 *
 * This backoff already adds randomness, so it is usually paired with `NoJitter`.
 *
 * After the first attempt, the next delay is computed as:
 * `random(initialDelay, min(maxDelay, lastAppliedDelay * 3))`
 */
public class DecorrelatedBackoff(
    public val initialDelay: Duration,
    public val maxDelay: Duration = Duration.INFINITE,
) : Backoff {

    init {
        requireFiniteNonNegative(initialDelay, "initialDelay")

        require(maxDelay > Duration.ZERO) {
            "maxDelay must be positive."
        }

        require(maxDelay >= initialDelay) {
            "maxDelay must not be less than initialDelay."
        }
    }

    override fun nextDelay(context: BackoffContext): Duration {
        val lastActualDelay = context.lastAppliedDelay ?: return initialDelay

        val upperBound = (lastActualDelay * 3).coerceAtMost(maxDelay)

        if (upperBound <= initialDelay) {
            return initialDelay
        }

        return initialDelay + (upperBound - initialDelay) * Random.nextDouble()
    }
}
