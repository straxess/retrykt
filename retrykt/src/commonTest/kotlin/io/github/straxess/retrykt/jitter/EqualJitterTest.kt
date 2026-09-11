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
    fun `returns zero for zero backoff delay`() {
        assertEquals(
            Duration.ZERO,
            EqualJitter.apply(Duration.ZERO),
        )
    }

    @Test
    fun `returns delay in equal jitter range`() {
        val backoffDelay = 10.milliseconds
        val minimumDelay = backoffDelay / 2
        val random = Random(0)

        repeat(1_000) {
            val actual = EqualJitter.apply(backoffDelay, random)

            assertTrue(actual >= minimumDelay)
            assertTrue(actual <= backoffDelay)
        }
    }

    @Test
    fun `returns randomized delay`() {
        val backoffDelay = 100.milliseconds
        val random = Random(0)

        val delays = buildSet {
            repeat(1_000) {
                add(EqualJitter.apply(backoffDelay, random))
            }
        }

        assertTrue(delays.size > 1)
    }

    @Test
    fun `throws IllegalArgumentException for negative backoff delay`() {
        assertFailsWith<IllegalArgumentException> {
            EqualJitter.apply((-100).milliseconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException for infinite backoff delay`() {
        assertFailsWith<IllegalArgumentException> {
            EqualJitter.apply(Duration.INFINITE)
        }
    }

    @Test
    fun `handles sub-divisible backoff delays`() {
        assertEquals(Duration.ZERO, EqualJitter.apply(1.nanoseconds))
        val random = Random(0)

        repeat(1_000) {
            val actual = EqualJitter.apply(2.nanoseconds, random)

            assertTrue(actual >= 1.nanoseconds)
            assertTrue(actual <= 2.nanoseconds)
        }
    }
}
