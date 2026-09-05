package io.github.straxess.retrykt.backoff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ExponentialBackoffTest {

    @Test
    fun `returns zero when initial delay is zero`() {
        val backoff = ExponentialBackoff(
            initialDelay = Duration.ZERO,
            maxDelay = 10.seconds,
        )

        val actual = backoff.nextDelay(
            BackoffContext(
                attempt = Int.MAX_VALUE,
                lastAppliedDelay = null,
            ),
        )

        assertEquals(Duration.ZERO, actual)
    }

    @Test
    fun `returns max delay when initial and max delay are equal`() {
        val backoff = ExponentialBackoff(
            initialDelay = 1.seconds,
            maxDelay = 1.seconds,
        )

        val actual = backoff.nextDelay(
            BackoffContext(
                attempt = Int.MAX_VALUE,
                lastAppliedDelay = null,
            ),
        )

        assertEquals(1.seconds, actual)
    }

    @Test
    fun `does not overflow for max attempt`() {
        val maxDelay = 1.hours

        val backoff = ExponentialBackoff(
            initialDelay = 1.milliseconds,
            multiplier = 2.0,
            maxDelay = maxDelay,
        )

        val actual = backoff.nextDelay(
            BackoffContext(
                attempt = Int.MAX_VALUE,
                lastAppliedDelay = null,
            ),
        )

        assertEquals(maxDelay, actual)
    }

    @Test
    fun `allows unbounded exponential delay`() {
        val backoff = ExponentialBackoff(
            initialDelay = 1.seconds,
            multiplier = 2.0,
            maxDelay = Duration.INFINITE,
        )

        assertEquals(
            1.seconds,
            backoff.nextDelay(BackoffContext(attempt = 1, lastAppliedDelay = null)),
        )

        assertEquals(
            2.seconds,
            backoff.nextDelay(BackoffContext(attempt = 2, lastAppliedDelay = null)),
        )

        assertEquals(
            4.seconds,
            backoff.nextDelay(BackoffContext(attempt = 3, lastAppliedDelay = null)),
        )
    }

    @Test
    fun `returns infinite when unbounded exponential delay overflows`() {
        val backoff = ExponentialBackoff(
            initialDelay = 1.seconds,
            multiplier = 2.0,
            maxDelay = Duration.INFINITE,
        )

        val actual = backoff.nextDelay(
            BackoffContext(
                attempt = Int.MAX_VALUE,
                lastAppliedDelay = null,
            ),
        )

        assertEquals(Duration.INFINITE, actual)
    }

    @Test
    fun `returns constant delay when multiplier is one`() {
        val backoff = ExponentialBackoff(
            initialDelay = 10.seconds,
            multiplier = 1.0,
            maxDelay = 20.seconds,
        )

        assertEquals(10.seconds, backoff.nextDelay(BackoffContext(1, null)))
        assertEquals(10.seconds, backoff.nextDelay(BackoffContext(2, null)))
        assertEquals(10.seconds, backoff.nextDelay(BackoffContext(100, null)))
    }

    @Test
    fun `calculates exponential delays`() {
        val backoff = ExponentialBackoff(10.seconds, 2.0)

        val firstDelay = backoff.nextDelay(BackoffContext(1, null))
        val secondDelay = backoff.nextDelay(BackoffContext(2, null))
        val thirdDelay = backoff.nextDelay(BackoffContext(3, null))

        assertEquals(10.seconds, firstDelay)
        assertEquals(20.seconds, secondDelay)
        assertEquals(40.seconds, thirdDelay)
    }

    @Test
    fun `respects max delay when exponential value reaches cap exactly`() {
        val backoff = ExponentialBackoff(
            initialDelay = 1.seconds,
            multiplier = 2.0,
            maxDelay = 8.seconds,
        )

        assertEquals(1.seconds, backoff.nextDelay(BackoffContext(1, null)))
        assertEquals(2.seconds, backoff.nextDelay(BackoffContext(2, null)))
        assertEquals(4.seconds, backoff.nextDelay(BackoffContext(3, null)))
        assertEquals(8.seconds, backoff.nextDelay(BackoffContext(4, null)))
        assertEquals(8.seconds, backoff.nextDelay(BackoffContext(5, null)))
    }

    @Test
    fun `respects max delay between exponential values`() {
        val backoff = ExponentialBackoff(
            initialDelay = 1.seconds,
            multiplier = 2.0,
            maxDelay = 5.seconds,
        )

        assertEquals(1.seconds, backoff.nextDelay(BackoffContext(1, null)))
        assertEquals(2.seconds, backoff.nextDelay(BackoffContext(2, null)))
        assertEquals(4.seconds, backoff.nextDelay(BackoffContext(3, null)))
        assertEquals(5.seconds, backoff.nextDelay(BackoffContext(4, null)))
        assertEquals(5.seconds, backoff.nextDelay(BackoffContext(5, null)))
    }

    @Test
    fun `throws IllegalArgumentException if initialDelay is less than 0`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff((-10).seconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException for infinite initial delay`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(initialDelay = Duration.INFINITE)
        }
    }

    @Test
    fun `throws IllegalArgumentException if multiplier is less than 1`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(10.seconds, 0.99)
        }
    }

    @Test
    fun `throws IllegalArgumentException if multiplier is negative infinity`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(10.seconds, Double.NEGATIVE_INFINITY)
        }
    }

    @Test
    fun `throws IllegalArgumentException if multiplier is positive infinity`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(10.seconds, Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun `throws IllegalArgumentException if multiplier is NaN`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(10.seconds, Double.NaN)
        }
    }

    @Test
    fun `throws IllegalArgumentException if maxDelay is less than 0`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(10.seconds, maxDelay = (-10).seconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException when max delay is less than initial delay`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(initialDelay = 100.milliseconds, maxDelay = 99.milliseconds)
        }
    }
}
