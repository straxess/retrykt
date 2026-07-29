package io.github.straxess.retrykt.backoff

import kotlin.time.Duration

public interface Backoff {

    public fun nextDelay(attempt: Int): Duration
}
