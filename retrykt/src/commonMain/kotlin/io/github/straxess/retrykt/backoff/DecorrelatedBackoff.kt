package io.github.straxess.retrykt.backoff

import kotlin.random.Random
import kotlin.time.Duration

/**
 * A randomized backoff strategy based on the AWS Decorrelated Jitter algorithm.
 *
 * Unlike deterministic backoff strategies, this implementation already
 * incorporates randomized delays and is typically used with `NoJitter`.
 *
 * The next delay is computed as:
 *
 * `random(initialDelay, min(maxDelay, lastActualDelay * 3))`
 */
public class DecorrelatedBackoff(
    public val initialDelay: Duration,
    public val maxDelay: Duration = Duration.INFINITE,
) : Backoff {

    init {
        require(initialDelay >= Duration.ZERO) {
            "initialDelay must be non-negative."
        }

        require(initialDelay.isFinite()) {
            "initialDelay must be finite."
        }

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
