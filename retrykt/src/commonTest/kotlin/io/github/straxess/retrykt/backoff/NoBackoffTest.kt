package io.github.straxess.retrykt.backoff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration

class NoBackoffTest {

    @Test
    fun `returns zero delay`() {
        val backoff = NoBackoff

        val firstDelay = backoff.nextDelay(0)
        val secondDelay = backoff.nextDelay(1)

        assertEquals(Duration.ZERO, firstDelay)
        assertEquals(Duration.ZERO, secondDelay)
        assertEquals(Duration.ZERO, secondDelay)
    }
}
