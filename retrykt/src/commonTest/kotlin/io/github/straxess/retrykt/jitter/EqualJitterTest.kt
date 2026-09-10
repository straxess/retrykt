package io.github.straxess.retrykt.jitter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

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
        val rawDelay = 10.milliseconds
        val minimumDelay = rawDelay / 2

        repeat(1_000_000) {
            val actual = EqualJitter.apply(rawDelay)

            assertTrue(actual >= minimumDelay)
            assertTrue(actual <= rawDelay)
        }
    }

    @Test
    fun `returns randomized delay`() {
        val rawDelay = 100.milliseconds

        val delays = buildSet {
            repeat(1_000_000) {
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

    @Test
    fun `handles sub-divisible raw delays`() {
        assertEquals(Duration.ZERO, EqualJitter.apply(1.nanoseconds))

        repeat(1_000_000) {
            val actual = EqualJitter.apply(2.nanoseconds)

            assertTrue(actual >= 1.nanoseconds)
            assertTrue(actual <= 2.nanoseconds)
        }
    }
}
