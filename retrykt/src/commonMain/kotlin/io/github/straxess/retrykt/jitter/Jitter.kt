package io.github.straxess.retrykt.jitter

import kotlin.time.Duration

public interface Jitter {

    public fun apply(baseDelay: Duration): Duration
}
