package io.github.straxess.retrykt.jitter

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
        val baseDelay = 10.seconds

        var hasJitter = false
        repeat(1_000) {
            val actual = jitter.apply(baseDelay)

            assertTrue(actual >= baseDelay)
            assertTrue(actual <= (baseDelay + maxJitter))

            if (actual > baseDelay) {
                hasJitter = true
            }
        }

        assertTrue(hasJitter)
    }

    @Test
    fun `supports zero maxJitter`() {
        val jitter = AdditiveJitter(Duration.ZERO)
        val baseDelay = 10.seconds

        val actual = jitter.apply(baseDelay)

        assertEquals(baseDelay, actual)
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
    fun `throws IllegalArgumentException for negative raw delay`() {
        assertFailsWith<IllegalArgumentException> {
            AdditiveJitter(100.milliseconds).apply((-100).milliseconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException for infinite raw delay`() {
        assertFailsWith<IllegalArgumentException> {
            AdditiveJitter(100.milliseconds).apply(Duration.INFINITE)
        }
    }

    @Test
    fun `throws IllegalStateException when complete jitter range is infinite`() {
        val rawDelay = ((Long.MAX_VALUE / 2) - 1).milliseconds
        val maxJitter = 1.milliseconds

        assertTrue(rawDelay.isFinite())
        assertTrue((rawDelay + maxJitter).isInfinite())

        val exception = assertFailsWith<IllegalStateException> {
            AdditiveJitter(maxJitter).apply(rawDelay)
        }

        assertEquals("rawDelay + maxJitter must be finite.", exception.message)
    }
}
