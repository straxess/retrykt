package io.github.straxess.retrykt.backoff

import io.github.straxess.retrykt.jitter.ConstantJitter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ConstantBackoffTest {

    @Test
    fun `returns constant delay`() {
        val backoff = ConstantBackoff(10.seconds)

        val firstDelay = backoff.nextDelay(0)
        val secondDelay = backoff.nextDelay(1)

        assertEquals(10.seconds, firstDelay)
        assertEquals(10.seconds, secondDelay)
    }

    @Test
    fun `ConstantBackoff applies jitter`() {
        val backoff = ConstantBackoff(10.seconds, ConstantJitter(100.milliseconds))

        val firstDelay = backoff.nextDelay(0)
        val secondDelay = backoff.nextDelay(1)

        assertEquals(10.seconds + 100.milliseconds, firstDelay)
        assertEquals(10.seconds + 100.milliseconds, secondDelay)
    }

    @Test
    fun `ConstantBackoff throws IllegalArgumentException if constant delay is less than 0`() {
        assertFailsWith<IllegalArgumentException> {
            ConstantBackoff((-10).seconds)
        }
    }
}
