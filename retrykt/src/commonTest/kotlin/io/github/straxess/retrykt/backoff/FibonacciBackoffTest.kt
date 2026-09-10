package io.github.straxess.retrykt.backoff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class FibonacciBackoffTest {

    @Test
    fun `returns zero when initial and max delays are zero`() {
        val backoff = FibonacciBackoff(initialDelay = Duration.ZERO, maxDelay = Duration.ZERO)
        val backoffContext = BackoffContext(attempt = Int.MAX_VALUE, prevAppliedDelay = null)

        val actual = backoff.nextDelay(backoffContext)

        assertEquals(Duration.ZERO, actual)
    }

    @Test
    fun `returns zero without iterating when initial delay is zero and max delay is positive`() {
        val backoff = FibonacciBackoff(initialDelay = Duration.ZERO, maxDelay = 1.days)
        val backoffContext = BackoffContext(attempt = Int.MAX_VALUE, prevAppliedDelay = null)

        val actual = backoff.nextDelay(backoffContext)

        assertEquals(Duration.ZERO, actual)
    }

    @Test
    fun `returns fibonacci sequence`() {
        val backoff = FibonacciBackoff(1.seconds, 1.days)

        assertEquals(1.seconds, backoff.nextDelay(BackoffContext(1, null)))
        assertEquals(1.seconds, backoff.nextDelay(BackoffContext(2, null)))
        assertEquals(2.seconds, backoff.nextDelay(BackoffContext(3, null)))
        assertEquals(3.seconds, backoff.nextDelay(BackoffContext(4, null)))
        assertEquals(5.seconds, backoff.nextDelay(BackoffContext(5, null)))
        assertEquals(8.seconds, backoff.nextDelay(BackoffContext(6, null)))
    }

    @Test
    fun `respects maxDelay`() {
        val backoff = FibonacciBackoff(initialDelay = 10.seconds, maxDelay = 15.seconds)

        assertEquals(10.seconds, backoff.nextDelay(BackoffContext(1, null)))
        assertEquals(10.seconds, backoff.nextDelay(BackoffContext(2, null)))
        assertEquals(15.seconds, backoff.nextDelay(BackoffContext(3, null)))
        assertEquals(15.seconds, backoff.nextDelay(BackoffContext(4, null)))
    }

    @Test
    fun `does not overflow for max attempt`() {
        val maxDelay = 1.hours
        val backoff = FibonacciBackoff(initialDelay = 1.milliseconds, maxDelay = maxDelay)
        val backoffContext = BackoffContext(attempt = Int.MAX_VALUE, prevAppliedDelay = null)

        val actual = backoff.nextDelay(backoffContext)

        assertEquals(maxDelay, actual)
    }

    @Test
    fun `does not overflow to infinite delay`() {
        val maxDelay = ((Long.MAX_VALUE / 2) - 1).milliseconds
        assertTrue(maxDelay.isFinite())
        assertTrue((maxDelay * 2).isInfinite())

        val initialDelay = maxDelay - 1.milliseconds
        val backoff = FibonacciBackoff(initialDelay = initialDelay, maxDelay = maxDelay)
        val backoffContext = BackoffContext(attempt = 3, prevAppliedDelay = null)

        val actual = backoff.nextDelay(backoffContext)

        assertEquals(maxDelay, actual)
    }

    @Test
    fun `throws IllegalArgumentException if initialDelay is less than 0`() {
        assertFailsWith<IllegalArgumentException> {
            FibonacciBackoff((-10).seconds, 1.days)
        }
    }

    @Test
    fun `throws IllegalArgumentException if infinite constant delay`() {
        assertFailsWith<IllegalArgumentException> {
            FibonacciBackoff(Duration.INFINITE, 1.days)
        }
    }

    @Test
    fun `throws IllegalArgumentException if maxDelay is less than 0`() {
        assertFailsWith<IllegalArgumentException> {
            FibonacciBackoff(10.seconds, maxDelay = (-10).seconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException if infinite maxDelay`() {
        assertFailsWith<IllegalArgumentException> {
            FibonacciBackoff(1.days, Duration.INFINITE)
        }
    }

    @Test
    fun `throws IllegalArgumentException when max delay is less than initial delay`() {
        assertFailsWith<IllegalArgumentException> {
            FibonacciBackoff(initialDelay = 2.seconds, maxDelay = 1.seconds)
        }
    }
}
