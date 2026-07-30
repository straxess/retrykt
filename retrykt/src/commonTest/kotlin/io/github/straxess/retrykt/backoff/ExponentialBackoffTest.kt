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

        val firstDelay = backoff.nextDelay(0)
        val secondDelay = backoff.nextDelay(1)
        val thirdDelay = backoff.nextDelay(2)

        assertEquals(10.seconds, firstDelay)
        assertEquals(20.seconds, secondDelay)
        assertEquals(40.seconds, thirdDelay)
    }

    @Test
    fun `ExponentialBackoff applies jitter`() {
        val backoff = ExponentialBackoff(10.seconds, 2.0, ConstantJitter(100.milliseconds))

        val firstDelay = backoff.nextDelay(0)
        val secondDelay = backoff.nextDelay(1)

        assertEquals(10.seconds + 100.milliseconds, firstDelay)
        assertEquals(20.seconds + 100.milliseconds, secondDelay)
    }

    @Test
    fun `ExponentialBackoff throws IllegalArgumentException if initialDelay is less than 0`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff((-10).seconds, 2.0)
        }
    }

    @Test
    fun `ExponentialBackoff throws IllegalArgumentException if stepMultiplier is less than 1`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(10.seconds, 0.99)
        }
    }

    @Test
    fun `ExponentialBackoff throws IllegalArgumentException if stepMultiplier is negative infinity`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(10.seconds, Double.NEGATIVE_INFINITY)
        }
    }

    @Test
    fun `ExponentialBackoff throws IllegalArgumentException if stepMultiplier is positive infinity`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(10.seconds, Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun `ExponentialBackoff throws IllegalArgumentException if stepMultiplier is NaN`() {
        assertFailsWith<IllegalArgumentException> {
            ExponentialBackoff(10.seconds, Double.NaN)
        }
    }
}
