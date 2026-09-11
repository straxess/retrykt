package io.github.straxess.retrykt.jitter

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AdditiveJitterTest {

    @Test
    fun `returns delays in expected range and applies jitter`() {
        val maxJitter = 100.milliseconds
        val jitter = AdditiveJitter(maxJitter)
        val backoffDelay = 10.seconds
        val random = Random(0)

        var hasJitter = false
        repeat(1_000) {
            val actual = jitter.apply(backoffDelay, random)

            assertTrue(actual >= backoffDelay)
            assertTrue(actual <= (backoffDelay + maxJitter))

            if (actual > backoffDelay) {
                hasJitter = true
            }
        }

        assertTrue(hasJitter)
    }

    @Test
    fun `supports zero maxJitter`() {
        val jitter = AdditiveJitter(Duration.ZERO)
        val backoffDelay = 10.seconds

        val actual = jitter.apply(backoffDelay)

        assertEquals(backoffDelay, actual)
    }

    @Test
    fun `throws IllegalArgumentException if maxJitter is negative`() {
        assertFailsWith<IllegalArgumentException> {
            AdditiveJitter((-1).milliseconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException if maxJitter is infinite`() {
        assertFailsWith<IllegalArgumentException> {
            AdditiveJitter(Duration.INFINITE)
        }
    }

    @Test
    fun `throws IllegalArgumentException for negative backoff delay`() {
        assertFailsWith<IllegalArgumentException> {
            AdditiveJitter(100.milliseconds).apply((-100).milliseconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException for infinite backoff delay`() {
        assertFailsWith<IllegalArgumentException> {
            AdditiveJitter(100.milliseconds).apply(Duration.INFINITE)
        }
    }

    @Test
    fun `throws IllegalStateException when complete jitter range is infinite`() {
        val backoffDelay = ((Long.MAX_VALUE / 2) - 1).milliseconds
        val maxJitter = 1.milliseconds

        assertTrue(backoffDelay.isFinite())
        assertTrue((backoffDelay + maxJitter).isInfinite())

        val exception = assertFailsWith<IllegalStateException> {
            AdditiveJitter(maxJitter).apply(backoffDelay)
        }

        assertEquals("backoffDelay + maxJitter must be finite.", exception.message)
    }
}
