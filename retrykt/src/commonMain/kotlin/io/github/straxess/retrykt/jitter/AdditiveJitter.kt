package io.github.straxess.retrykt.jitter

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.random.Random
import kotlin.time.Duration

/**
 * Adds a random value from `0` to [maxJitter] to the backoff delay.
 *
 * This random part does not depend on the backoff delay. Duration rounding can include the upper bound. The complete
 * range must be finite; RetryKt rejects an overflow instead of shortening the range.
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
     * @throws IllegalArgumentException if [backoffDelay] is negative or infinite.
     * @throws IllegalStateException if `backoffDelay + maxJitter` is infinite.
     */
    override fun apply(backoffDelay: Duration): Duration = apply(backoffDelay, Random.Default)

    internal fun apply(backoffDelay: Duration, random: Random): Duration {
        requireFiniteNonNegative(backoffDelay, "backoffDelay")

        if (maxJitter == Duration.ZERO) {
            return backoffDelay
        }

        val upperBound = backoffDelay + maxJitter

        check(upperBound.isFinite()) {
            "backoffDelay + maxJitter must be finite."
        }

        return backoffDelay + (upperBound - backoffDelay) * random.nextDouble()
    }
}
