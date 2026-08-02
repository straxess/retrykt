package io.github.straxess.retrykt.jitter

import kotlin.time.Duration

class ConstantJitter(private val jitter: Duration) : Jitter {

    override fun apply(baseDelay: Duration) = baseDelay + jitter
}
