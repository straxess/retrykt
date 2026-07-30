package io.github.straxess.retrykt.jitter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class NoJitterTest {

    @Test
    fun `return original delay`() {
        val jitter = NoJitter
        val baseDelay = 10.seconds

        val actual = jitter.apply(baseDelay)

        assertEquals(baseDelay, actual)
    }
}
