package io.github.straxess.retrykt.jitter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class EqualJitterTest {

    @Test
    fun `returns zero for zero raw delay`() {
        assertEquals(
            Duration.ZERO,
            EqualJitter.apply(Duration.ZERO),
        )
    }

    @Test
    fun `returns delay in equal jitter range`() {
        val rawDelay = 100.milliseconds
        val minimumDelay = rawDelay / 2

        repeat(100) {
            val actual = EqualJitter.apply(rawDelay)

            assertTrue(actual >= minimumDelay)
            assertTrue(actual < rawDelay)
        }
    }

    @Test
    fun `returns randomized delay`() {
        val rawDelay = 100.milliseconds

        val delays = buildSet {
            repeat(100) {
                add(EqualJitter.apply(rawDelay))
            }
        }

        assertTrue(delays.size > 1)
    }

    @Test
    fun `throws IllegalArgumentException for negative raw delay`() {
        assertFailsWith<IllegalArgumentException> {
            EqualJitter.apply((-100).milliseconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException for infinite raw delay`() {
        assertFailsWith<IllegalArgumentException> {
            EqualJitter.apply(Duration.INFINITE)
        }
    }
}
