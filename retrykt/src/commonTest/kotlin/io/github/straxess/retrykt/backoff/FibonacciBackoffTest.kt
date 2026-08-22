package io.github.straxess.retrykt.backoff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

class FibonacciBackoffTest {

    @Test
    fun `returns fibonacci sequence`() {
        val backoff = FibonacciBackoff(1.seconds)

        assertEquals(1.seconds, backoff.nextDelay(BackoffContext(1, null)))
        assertEquals(1.seconds, backoff.nextDelay(BackoffContext(2, null)))
        assertEquals(2.seconds, backoff.nextDelay(BackoffContext(3, null)))
        assertEquals(3.seconds, backoff.nextDelay(BackoffContext(4, null)))
        assertEquals(5.seconds, backoff.nextDelay(BackoffContext(5, null)))
        assertEquals(8.seconds, backoff.nextDelay(BackoffContext(6, null)))
    }

    @Test
    fun `respects maxDelay`() {
        val backoff = FibonacciBackoff(
            initialDelay = 10.seconds,
            maxDelay = 15.seconds,
        )

        assertEquals(10.seconds, backoff.nextDelay(BackoffContext(1, null)))
        assertEquals(10.seconds, backoff.nextDelay(BackoffContext(2, null)))
        assertEquals(15.seconds, backoff.nextDelay(BackoffContext(3, null)))
        assertEquals(15.seconds, backoff.nextDelay(BackoffContext(4, null)))
    }

    @Test
    fun `throws IllegalArgumentException if initialDelay is less than 0`() {
        assertFailsWith<IllegalArgumentException> {
            FibonacciBackoff((-10).seconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException if maxDelay is less than 0`() {
        assertFailsWith<IllegalArgumentException> {
            FibonacciBackoff(10.seconds, maxDelay = (-10).seconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException when max delay is less than initial delay`() {
        assertFailsWith<IllegalArgumentException> {
            FibonacciBackoff(initialDelay = 2.seconds, maxDelay = 1.seconds)
        }
    }
}
