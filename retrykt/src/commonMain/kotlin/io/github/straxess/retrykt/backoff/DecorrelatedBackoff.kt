package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.random.Random
import kotlin.time.Duration

/**
 * Randomized backoff based on the AWS decorrelated-jitter algorithm.
 *
 * This backoff already adds randomness, so it is usually paired with `NoJitter`.
 *
 * The first retry uses `initialDelay`. Subsequent retry delays are computed as:
 * `random(initialDelay, min(maxDelay, prevAppliedDelay * 3))`.
 * Requires `0 <= initialDelay <= maxDelay < Duration.INFINITE`.
 */
public class DecorrelatedBackoff(
    public val initialDelay: Duration,
    public val maxDelay: Duration,
) : Backoff {

    init {
        requireFiniteNonNegative(initialDelay, "initialDelay")
        requireFiniteNonNegative(maxDelay, "maxDelay")

        require(maxDelay >= initialDelay) {
            "maxDelay must not be less than initialDelay."
        }
    }

    override fun nextDelay(context: BackoffContext): Duration {
        val prevAppliedDelay = context.prevAppliedDelay ?: return initialDelay
        val upperBound = if (prevAppliedDelay > maxDelay / 3) maxDelay else prevAppliedDelay * 3

        if (upperBound <= initialDelay) {
            return initialDelay
        }

        return initialDelay + (upperBound - initialDelay) * Random.nextDouble()
    }
}
