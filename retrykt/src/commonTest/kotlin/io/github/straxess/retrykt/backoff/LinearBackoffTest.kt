package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.jitter.ConstantJitter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LinearBackoffTest {

    @Test
    fun `returns linear delay`() {
        val backoff = LinearBackoff(10.seconds)

        val firstDelay = backoff.nextDelay(1)
        val secondDelay = backoff.nextDelay(2)
        val thirdDelay = backoff.nextDelay(3)

        assertEquals(10.seconds, firstDelay)
        assertEquals(20.seconds, secondDelay)
        assertEquals(30.seconds, thirdDelay)
    }

    @Test
    fun `LinearBackoff applies jitter`() {
        val backoff = LinearBackoff(10.seconds, ConstantJitter(100.milliseconds))

        val firstDelay = backoff.nextDelay(1)
        val secondDelay = backoff.nextDelay(2)

        assertEquals(10.seconds + 100.milliseconds, firstDelay)
        assertEquals(20.seconds + 100.milliseconds, secondDelay)
    }

    @Test
    fun `LinearBackoff throws IllegalArgumentException if increment is less than 0`() {
        assertFailsWith<IllegalArgumentException> {
            LinearBackoff((-10).seconds)
        }
    }
}
