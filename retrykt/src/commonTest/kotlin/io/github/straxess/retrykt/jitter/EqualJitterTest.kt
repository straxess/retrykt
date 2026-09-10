package io.github.straxess.retrykt.jitter

import kotlin.random.Random
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
        val random = Random(0)

        repeat(1_000) {
            val actual = EqualJitter.apply(rawDelay, random)

            assertTrue(actual >= minimumDelay)
            assertTrue(actual <= rawDelay)
        }
    }

    @Test
    fun `returns randomized delay`() {
        val rawDelay = 100.milliseconds
        val random = Random(0)

        val delays = buildSet {
            repeat(1_000) {
                add(EqualJitter.apply(rawDelay, random))
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
        val random = Random(0)

        repeat(1_000) {
            val actual = EqualJitter.apply(2.nanoseconds, random)

            assertTrue(actual >= 1.nanoseconds)
            assertTrue(actual <= 2.nanoseconds)
        }
    }
}
