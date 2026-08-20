package io.github.straxess.retrykt.jitter

import kotlin.time.Duration

/**
 * Leaves the backoff delay unchanged.
 */
public object NoJitter : Jitter {

    override fun apply(rawDelay: Duration): Duration = rawDelay
}
