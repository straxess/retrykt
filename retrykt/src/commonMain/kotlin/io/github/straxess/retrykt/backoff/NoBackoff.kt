package io.github.straxess.retrykt.backoff

import kotlin.time.Duration

public object NoBackoff : Backoff {

    override fun nextDelay(attempt: Int): Duration {
        return Duration.ZERO
    }
}
