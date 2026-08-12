package io.github.straxess.retrykt.backoff

import kotlin.time.Duration

/**
 * Always returns `Duration.ZERO`.
 */
public object NoBackoff : Backoff {

    override fun nextDelay(context: BackoffContext): Duration {
        return Duration.ZERO
    }
}
