package io.github.straxess.retrykt.backoff

import kotlin.time.Duration

public interface Backoff {

    /**
     * [attempt] starts from 1
     */
    public fun nextDelay(attempt: Int): Duration
}
