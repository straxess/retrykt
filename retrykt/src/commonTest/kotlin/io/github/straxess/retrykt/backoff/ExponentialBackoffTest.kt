package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.jitter.ConstantJitter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ExponentialBackoffTest {

    @Test
    fun `returns exponential delay`() {
        val backoff = ExponentialBackoff(10.seconds, 2.0)

        val firstDelay = backoff.nextDelay(1)
        val secondDelay = backoff.nextDelay(2)
        val thirdDelay = backoff.nextDelay(3)

        assertEquals(10.seconds, firstDelay)
        assertEquals(20.seconds, secondDelay)
        assertEquals(40.seconds, thirdDelay)
    }

    @Test
    fun `ExponentialBackoff applies jitter`() {
        val backoff = ExponentialBackoff(
            initialDelay = 10.seconds,
            multiplier = 2.0,
            jitter = ConstantJitter(100.milliseconds),
        )

        val firstDelay = backoff.nextDelay(1)
        val secondDelay = backoff.nextDelay(2)

        assertEquals(10.seconds + 100.milliseconds, firstDelay)
        assertEquals(20.seconds + 100.milliseconds, secondDelay)
    }

    @Test
    fun `ExponentialBackoff caps delay from first attempt`() {
        val backoff = ExponentialBackoff(
            initialDelay = 1.seconds,
            multiplier = 2.0,
            maxDelay = 500.milliseconds,
        )

        assertEquals(500.milliseconds, backoff.nextDelay(1))
        assertEquals(500.milliseconds, backoff.nextDelay(2))
    }

    @Test
    fun `ExponentialBackoff respects max delay`() {
        val backoff = ExponentialBackoff(
            initialDelay = 100.milliseconds,
            multiplier = 2.0,
            maxDelay = 500.milliseconds,
        )

        assertEquals(100.milliseconds, backoff.nextDelay(1))
        assertEquals(200.milliseconds, backoff.nextDelay(2))
        assertEquals(400.milliseconds, backoff.nextDelay(3))
        assertEquals(500.milliseconds, backoff.nextDelay(4))
        assertEquals(500.milliseconds, backoff.nextDelay(5))
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
}
