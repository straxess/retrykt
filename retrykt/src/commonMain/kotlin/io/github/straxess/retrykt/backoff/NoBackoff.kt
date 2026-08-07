package io.github.straxess.retrykt.backoff

import kotlin.time.Duration

public object NoBackoff : Backoff {

    override fun nextDelay(context: BackoffContext): Duration {
        return Duration.ZERO
    }
}
