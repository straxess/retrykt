package io.github.straxess.retrykt.backoff

import kotlin.time.Duration

/**
 * Retries immediately, without a backoff delay.
 */
public object NoBackoff : Backoff {

    override fun calculateDelay(context: BackoffContext): Duration = Duration.ZERO
}
