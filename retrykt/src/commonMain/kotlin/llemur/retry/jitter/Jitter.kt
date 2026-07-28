package llemur.retry.jitter

import kotlin.time.Duration

interface Jitter {

    fun apply(baseDelay: Duration): Duration
}
