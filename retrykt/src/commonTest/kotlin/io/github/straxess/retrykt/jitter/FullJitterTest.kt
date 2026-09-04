package io.github.straxess.retrykt.jitter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class FullJitterTest {

    @Test
    fun `returns zero for zero raw delay`() {
        assertEquals(
            Duration.ZERO,
            FullJitter.apply(Duration.ZERO),
        )
    }

    @Test
    fun `returns delay in range`() {
        val rawDelay = 100.milliseconds

        repeat(100) {
            val actual = FullJitter.apply(rawDelay)

            assertTrue(actual >= Duration.ZERO)
            assertTrue(actual < rawDelay)
        }
    }

    @Test
    fun `returns randomized delay`() {
        val rawDelay = 100.milliseconds

        val delays = buildSet {
            repeat(100) {
                add(FullJitter.apply(rawDelay))
            }
        }

        assertTrue(delays.size > 1)
    }

    @Test
    fun `throws IllegalArgumentException for negative raw delay`() {
        assertFailsWith<IllegalArgumentException> {
            FullJitter.apply((-100).milliseconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException for infinite raw delay`() {
        assertFailsWith<IllegalArgumentException> {
            FullJitter.apply(Duration.INFINITE)
        }
    }
}
