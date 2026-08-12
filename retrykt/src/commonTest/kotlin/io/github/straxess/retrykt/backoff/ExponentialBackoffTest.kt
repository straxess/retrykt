package io.github.straxess.retrykt.backoff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ExponentialBackoffTest {

    @Test
    fun `returns exponential delay`() {
        val backoff = ExponentialBackoff(10.seconds, 2.0)

        val firstDelay = backoff.nextDelay(BackoffContext(1, null))
        val secondDelay = backoff.nextDelay(BackoffContext(2, null))
        val thirdDelay = backoff.nextDelay(BackoffContext(3, null))

        assertEquals(10.seconds, firstDelay)
        assertEquals(20.seconds, secondDelay)
        assertEquals(40.seconds, thirdDelay)
    }

    @Test
    fun `ExponentialBackoff respects max delay`() {
        val backoff = ExponentialBackoff(
            initialDelay = 100.milliseconds,
            multiplier = 2.0,
            maxDelay = 500.milliseconds,
        )

        assertEquals(100.milliseconds, backoff.nextDelay(BackoffContext(1, null)))
        assertEquals(200.milliseconds, backoff.nextDelay(BackoffContext(2, null)))
        assertEquals(400.milliseconds, backoff.nextDelay(BackoffContext(3, null)))
        assertEquals(500.milliseconds, backoff.nextDelay(BackoffContext(4, null)))
        assertEquals(500.milliseconds, backoff.nextDelay(BackoffContext(5, null)))
    }

    @Test
    fun `ExponentialBackoff throws IllegalArgumentException if initialDelay is less than 0`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff((-10).seconds)
        }
    }

    @Test
    fun `ExponentialBackoff throws IllegalArgumentException if multiplier is less than 1`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(10.seconds, 0.99)
        }
    }

    @Test
    fun `ExponentialBackoff throws IllegalArgumentException if multiplier is negative infinity`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(10.seconds, Double.NEGATIVE_INFINITY)
        }
    }

    @Test
    fun `ExponentialBackoff throws IllegalArgumentException if multiplier is positive infinity`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(10.seconds, Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun `ExponentialBackoff throws IllegalArgumentException if multiplier is NaN`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(10.seconds, Double.NaN)
        }
    }

    @Test
    fun `ExponentialBackoff throws IllegalArgumentException if maxDelay is less than 0`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(10.seconds, maxDelay = (-10).seconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException when max delay is less than initial delay`() {
        assertFailsWith<IllegalArgumentException> {
            DecorrelatedBackoff(initialDelay = 100.milliseconds, maxDelay = 99.milliseconds)
        }
    }
}
