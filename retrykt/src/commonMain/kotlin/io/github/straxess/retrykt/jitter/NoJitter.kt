package io.github.straxess.retrykt.jitter

import kotlin.time.Duration

public object NoJitter : Jitter {

    override fun apply(baseDelay: Duration): Duration {
        return baseDelay
    }
}
