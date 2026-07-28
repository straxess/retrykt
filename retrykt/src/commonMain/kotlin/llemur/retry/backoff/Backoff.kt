package llemur.retry.backoff

import kotlin.time.Duration

interface Backoff {

    fun nextDelay(attempt: Int): Duration
}
