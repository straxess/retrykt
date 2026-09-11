package io.github.straxess.retrykt.jitter

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

class FullJitterTest {

    @Test
    fun `returns zero for zero backoff delay`() {
        assertEquals(
            Duration.ZERO,
            FullJitter().apply(Duration.ZERO),
        )
    }

    @Test
    fun `returns delay in range`() {
        val backoffDelay = 100.milliseconds
        val jitter = FullJitter(random = Random(0))

        repeat(1_000) {
            val actual = jitter.apply(backoffDelay)

            assertTrue(actual >= Duration.ZERO)
            assertTrue(actual <= backoffDelay)
        }
    }

    @Test
    fun `returns randomized delay`() {
        val backoffDelay = 100.milliseconds
        val jitter = FullJitter(random = Random(0))

        val delays = buildSet {
            repeat(1_000) {
                add(jitter.apply(backoffDelay))
            }
        }

        assertTrue(delays.size > 1)
    }

    @Test
    fun `throws IllegalArgumentException for negative backoff delay`() {
        assertFailsWith<IllegalArgumentException> {
            FullJitter().apply((-100).milliseconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException for infinite backoff delay`() {
        assertFailsWith<IllegalArgumentException> {
            FullJitter().apply(Duration.INFINITE)
        }
    }

    @Test
    fun `keeps minimum representable delay within bounds`() {
        val jitter = FullJitter(random = Random(0))

        repeat(1_000) {
            val actual = jitter.apply(1.nanoseconds)

            assertTrue(actual >= Duration.ZERO)
            assertTrue(actual <= 1.nanoseconds)
        }
    }
}
