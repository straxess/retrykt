package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.random.Random
import kotlin.time.Duration

/**
 * Calculates randomized delays with the AWS decorrelated-jitter algorithm.
 *
 * This strategy already adds randomness and normally does not need another jitter.
 *
 * The first attempt starts immediately. The delay before the first retry is [firstDelay]. Later delays are calculated
 * as:
 * `random(firstDelay, min(maxDelay, prevAppliedDelay * 3))`.
 * Both configured delays must be finite and `0 <= firstDelay <= maxDelay`.
 * This backoff reuses [random] on every call. If the backoff is shared between concurrent operations, [random] must
 * support concurrent calls.
 *
 * @throws IllegalArgumentException if a delay is negative or infinite, or [firstDelay] is greater than [maxDelay].
 */
public class DecorrelatedBackoff(
    public val firstDelay: Duration,
    public val maxDelay: Duration,
    private val random: Random = Random.Default,
) : Backoff {

    init {
        requireFiniteNonNegative(firstDelay, "firstDelay")
        requireFiniteNonNegative(maxDelay, "maxDelay")

        require(maxDelay >= firstDelay) {
            "maxDelay must not be less than firstDelay."
        }
    }

    override fun calculateDelay(context: BackoffContext): Duration {
        val prevAppliedDelay = context.prevAppliedDelay ?: return firstDelay
        val upperBound = if (prevAppliedDelay > maxDelay / 3) maxDelay else prevAppliedDelay * 3

        if (upperBound <= firstDelay) {
            return firstDelay
        }

        return firstDelay + (upperBound - firstDelay) * random.nextDouble()
    }
}
