package io.github.straxess.retrykt.jitter

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.random.Random
import kotlin.time.Duration

/**
 * Returns a random delay from zero to [backoffDelay]. Duration rounding can include the upper bound.
 *
 * This jitter reuses [random] on every call. If the jitter is shared between concurrent operations, [random] must
 * support concurrent calls.
 */
public class FullJitter(
    private val random: Random = Random.Default,
) : Jitter {

    /**
     * @throws IllegalArgumentException if [backoffDelay] is negative or infinite.
     */
    override fun apply(backoffDelay: Duration): Duration {
        requireFiniteNonNegative(backoffDelay, "backoffDelay")

        if (backoffDelay == Duration.ZERO) {
            return Duration.ZERO
        }

        return backoffDelay * random.nextDouble()
    }
}
