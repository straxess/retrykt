package io.github.straxess.retrykt.backoff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration

class NoBackoffTest {

    @Test
    fun `returns zero delay`() {
        val backoff = NoBackoff

        val firstDelay = backoff.calculateDelay(BackoffContext(1, null))
        val secondDelay = backoff.calculateDelay(BackoffContext(2, null))

        assertEquals(Duration.ZERO, firstDelay)
        assertEquals(Duration.ZERO, secondDelay)
    }
}
