package io.github.straxess.retrykt.backoff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration

class NoBackoffTest {

    @Test
    fun `returns zero delay`() {
        val backoff = NoBackoff

        val firstDelay = backoff.nextDelay(BackoffContext(1, null))
        val secondDelay = backoff.nextDelay(BackoffContext(2, null))

        assertEquals(Duration.ZERO, firstDelay)
        assertEquals(Duration.ZERO, secondDelay)
    }
}
