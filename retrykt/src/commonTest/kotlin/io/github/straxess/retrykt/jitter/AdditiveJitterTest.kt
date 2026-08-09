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
    fun `returns delay in expected range`() {
        val maxJitter = 100.milliseconds
        val jitter = AdditiveJitter(maxJitter)
        val baseDelay = 10.seconds

        val actual = jitter.apply(baseDelay)

        assertTrue(actual >= baseDelay)
        assertTrue(actual < baseDelay + maxJitter)
    }

    @Test
    fun `returns delays in expected range for many invocations`() {
        val maxJitter = 100.milliseconds
        val jitter = AdditiveJitter(maxJitter)
        val baseDelay = 10.seconds

        repeat(10_000) {
            val actual = jitter.apply(baseDelay)

            assertTrue(actual >= baseDelay)
            assertTrue(actual < baseDelay + maxJitter)
        }
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
}
