package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.internal.requireFiniteNonNegative
import kotlin.time.Duration

/**
 * Increases the delay with the Fibonacci sequence.
 *
 * The first attempt starts immediately. The first two retry delays equal [firstDelay]. Each later delay is the sum of
 * the previous two, up to [maxDelay]. If [firstDelay] is zero, every delay is zero. Both configured delays must be
 * finite and `0 <= firstDelay <= maxDelay`.
 *
 * @throws IllegalArgumentException if a delay is negative or infinite, or [firstDelay] is greater than [maxDelay].
 */
public class FibonacciBackoff(
    public val firstDelay: Duration,
    public val maxDelay: Duration,
) : Backoff {

    init {
        requireFiniteNonNegative(firstDelay, "firstDelay")
        requireFiniteNonNegative(maxDelay, "maxDelay")

        require(maxDelay >= firstDelay) {
            "maxDelay must not be less than firstDelay."
        }
    }

    override fun calculateDelay(context: BackoffContext): Duration {
        if (firstDelay == Duration.ZERO) {
            return Duration.ZERO
        }

        var prevDelay = Duration.ZERO
        var delay = firstDelay

        repeat(context.attempt - 1) {
            if (delay >= maxDelay - prevDelay) {
                return maxDelay
            }

            val intermediate = delay
            delay += prevDelay
            prevDelay = intermediate
        }

        return delay
    }
}
