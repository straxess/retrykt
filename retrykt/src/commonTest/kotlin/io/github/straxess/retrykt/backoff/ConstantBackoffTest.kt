package io.github.straxess.retrykt.backoff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ConstantBackoffTest {

    @Test
    fun `returns constant delay`() {
        val backoff = ConstantBackoff(10.seconds)

        val firstDelay = backoff.nextDelay(BackoffContext(1, null))
        val secondDelay = backoff.nextDelay(BackoffContext(2, firstDelay))

        assertEquals(10.seconds, firstDelay)
        assertEquals(10.seconds, secondDelay)
    }

    @Test
    fun `throws IllegalArgumentException if constant delay is less than 0`() {
        assertFailsWith<IllegalArgumentException> {
            ConstantBackoff((-10).seconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException if infinite constant delay`() {
        assertFailsWith<IllegalArgumentException> {
            ConstantBackoff(Duration.INFINITE)
        }
    }
}
