package llemur.retry.jitter

import kotlin.time.Duration

object NoJitter : Jitter {

    override fun apply(rawDuration: Duration): Duration {
        return rawDuration
    }
}
