package io.github.straxess.retrykt.jitter

import kotlin.time.Duration

public fun interface Jitter {

    public fun apply(rawDelay: Duration): Duration
}
