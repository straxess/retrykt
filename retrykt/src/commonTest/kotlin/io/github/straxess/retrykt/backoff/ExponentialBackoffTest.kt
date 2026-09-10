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

class ExponentialBackoffTest {

    @Test
    fun `returns zero when initial delay is zero`() {
        val backoff =
            ExponentialBackoff(
                initialDelay = Duration.ZERO,
                maxDelay = Duration.ZERO,
            )

        val actual =
            backoff.nextDelay(
                BackoffContext(
                    attempt = Int.MAX_VALUE,
                    prevAppliedDelay = null,
                ),
            )

        assertEquals(Duration.ZERO, actual)
    }

    @Test
    fun `returns max delay when initial and max delay are equal`() {
        val backoff = ExponentialBackoff(initialDelay = 1.seconds, maxDelay = 1.seconds)
        val backoffContext = BackoffContext(attempt = Int.MAX_VALUE, prevAppliedDelay = null)

        val actual = backoff.nextDelay(backoffContext)

        assertEquals(1.seconds, actual)
    }

    @Test
    fun `does not overflow for max attempt`() {
        val maxDelay = 1.hours
        val backoff = ExponentialBackoff(initialDelay = 1.milliseconds, multiplier = 2.0, maxDelay = maxDelay)
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
        val backoff = ExponentialBackoff(initialDelay = initialDelay, multiplier = 2.0, maxDelay = maxDelay)
        val backoffContext = BackoffContext(attempt = 2, prevAppliedDelay = null)

        val actual = backoff.nextDelay(backoffContext)

        assertEquals(maxDelay, actual)
    }

    @Test
    fun `returns constant delay when multiplier is one`() {
        val backoff = ExponentialBackoff(initialDelay = 10.seconds, multiplier = 1.0, maxDelay = 20.seconds)

        assertEquals(10.seconds, backoff.nextDelay(BackoffContext(1, null)))
        assertEquals(10.seconds, backoff.nextDelay(BackoffContext(2, null)))
        assertEquals(10.seconds, backoff.nextDelay(BackoffContext(100, null)))
    }

    @Test
    fun `calculates exponential delays`() {
        val backoff = ExponentialBackoff(10.seconds, 1.days, 2.0)

        val firstDelay = backoff.nextDelay(BackoffContext(1, null))
        val secondDelay = backoff.nextDelay(BackoffContext(2, null))
        val thirdDelay = backoff.nextDelay(BackoffContext(3, null))

        assertEquals(10.seconds, firstDelay)
        assertEquals(20.seconds, secondDelay)
        assertEquals(40.seconds, thirdDelay)
    }

    @Test
    fun `calculates delays with fractional multiplier`() {
        val backoff = ExponentialBackoff(initialDelay = 1.seconds, maxDelay = 10.seconds, multiplier = 1.5)

        assertEquals(1.seconds, backoff.nextDelay(BackoffContext(1, null)))
        assertEquals(1_500.milliseconds, backoff.nextDelay(BackoffContext(2, null)))
        assertEquals(2_250.milliseconds, backoff.nextDelay(BackoffContext(3, null)))
    }

    @Test
    fun `caps delay when multiplier power overflows`() {
        val backoff = ExponentialBackoff(initialDelay = 1.seconds, maxDelay = 1.days, multiplier = Double.MAX_VALUE)

        assertEquals(1.days, backoff.nextDelay(BackoffContext(Int.MAX_VALUE, null)))
    }

    @Test
    fun `respects max delay when exponential value reaches cap exactly`() {
        val backoff = ExponentialBackoff(initialDelay = 1.seconds, multiplier = 2.0, maxDelay = 8.seconds)

        assertEquals(1.seconds, backoff.nextDelay(BackoffContext(1, null)))
        assertEquals(2.seconds, backoff.nextDelay(BackoffContext(2, null)))
        assertEquals(4.seconds, backoff.nextDelay(BackoffContext(3, null)))
        assertEquals(8.seconds, backoff.nextDelay(BackoffContext(4, null)))
        assertEquals(8.seconds, backoff.nextDelay(BackoffContext(5, null)))
    }

    @Test
    fun `respects max delay between exponential values`() {
        val backoff = ExponentialBackoff(initialDelay = 1.seconds, multiplier = 2.0, maxDelay = 5.seconds)

        assertEquals(1.seconds, backoff.nextDelay(BackoffContext(1, null)))
        assertEquals(2.seconds, backoff.nextDelay(BackoffContext(2, null)))
        assertEquals(4.seconds, backoff.nextDelay(BackoffContext(3, null)))
        assertEquals(5.seconds, backoff.nextDelay(BackoffContext(4, null)))
        assertEquals(5.seconds, backoff.nextDelay(BackoffContext(5, null)))
    }

    @Test
    fun `throws IllegalArgumentException if initialDelay is less than 0`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff((-10).seconds, maxDelay = 1.seconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException for infinite initial delay`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(initialDelay = Duration.INFINITE, maxDelay = 1.seconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException if multiplier is less than 1`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(10.seconds, 20.seconds, 0.99)
        }
    }

    @Test
    fun `throws IllegalArgumentException if multiplier is negative infinity`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(10.seconds, 20.seconds, Double.NEGATIVE_INFINITY)
        }
    }

    @Test
    fun `throws IllegalArgumentException if multiplier is positive infinity`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(10.seconds, 20.seconds, Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun `throws IllegalArgumentException if multiplier is NaN`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(10.seconds, 20.seconds, Double.NaN)
        }
    }

    @Test
    fun `throws IllegalArgumentException if maxDelay is less than 0`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(10.seconds, maxDelay = (-10).seconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException for infinite max delay`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(initialDelay = 1.seconds, maxDelay = Duration.INFINITE)
        }
    }

    @Test
    fun `throws IllegalArgumentException when max delay is less than initial delay`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(initialDelay = 100.milliseconds, maxDelay = 99.milliseconds)
        }
    }
}
