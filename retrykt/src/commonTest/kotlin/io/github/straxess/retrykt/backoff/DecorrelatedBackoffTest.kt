package io.github.straxess.retrykt.backoff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class DecorrelatedBackoffTest {

    @Test
    fun `returns initial delay on first attempt`() {
        val backoff = DecorrelatedBackoff(initialDelay = 100.milliseconds, maxDelay = 10.seconds)

        val actual = backoff.nextDelay(
            BackoffContext(attempt = 1, lastAppliedDelay = null),
        )

        assertEquals(100.milliseconds, actual)
    }

    @Test
    fun `returns delay between initial delay and three times last applied delay`() {
        val initialDelay = 100.milliseconds
        val lastAppliedDelay = 200.milliseconds

        val backoff = DecorrelatedBackoff(initialDelay = initialDelay, maxDelay = 10.seconds)

        repeat(100) {
            val actual = backoff.nextDelay(
                BackoffContext(attempt = 2, lastAppliedDelay = lastAppliedDelay),
            )

            assertTrue(actual >= initialDelay)
            assertTrue(actual <= lastAppliedDelay * 3)
        }
    }

    @Test
    fun `returns randomized delay`() {
        val initialDelay = 100.milliseconds
        val lastAppliedDelay = 200.milliseconds
        val maxDelay = 10.seconds

        val backoff = DecorrelatedBackoff(initialDelay = initialDelay, maxDelay = maxDelay)

        val delays = buildSet {
            repeat(100) {
                val backoffContext = BackoffContext(attempt = 2, lastAppliedDelay = lastAppliedDelay)
                add(backoff.nextDelay(backoffContext))
            }
        }

        assertTrue(delays.size > 1)
    }

    @Test
    fun `returns initial delay when last applied delay is below initial delay`() {
        val initialDelay = 100.milliseconds

        val backoff = DecorrelatedBackoff(initialDelay = initialDelay, maxDelay = 10.seconds)

        val actual = backoff.nextDelay(
            BackoffContext(attempt = 2, lastAppliedDelay = 20.milliseconds),
        )

        assertEquals(initialDelay, actual)
    }

    @Test
    fun `returns initial delay when max delay equals initial delay`() {
        val delay = 100.milliseconds

        val backoff = DecorrelatedBackoff(initialDelay = delay, maxDelay = delay)

        repeat(100) {
            val backoffContext = BackoffContext(attempt = 2, lastAppliedDelay = 200.milliseconds)
            val actual = backoff.nextDelay(backoffContext)

            assertEquals(delay, actual)
        }
    }

    @Test
    fun `returns randomized delay up to max delay when three times last applied delay exceeds max delay`() {
        val initialDelay = 100.milliseconds
        val maxDelay = 500.milliseconds
        val lastAppliedDelay = 200.milliseconds

        val backoff = DecorrelatedBackoff(initialDelay = initialDelay, maxDelay = maxDelay)

        repeat(100) {
            val backoffContext = BackoffContext(attempt = 2, lastAppliedDelay = lastAppliedDelay)
            val actual = backoff.nextDelay(backoffContext)

            assertTrue(actual >= initialDelay)
            assertTrue(actual <= maxDelay)
        }
    }

    @Test
    fun `returns randomized delay up to three times last applied delay when max delay is infinite`() {
        val initialDelay = 100.milliseconds
        val lastAppliedDelay = 200.milliseconds

        val backoff = DecorrelatedBackoff(
            initialDelay = initialDelay,
            maxDelay = Duration.INFINITE,
        )

        repeat(100) {
            val backoffContext = BackoffContext(attempt = 2, lastAppliedDelay = lastAppliedDelay)
            val actual = backoff.nextDelay(backoffContext)

            assertTrue(actual >= initialDelay)
            assertTrue(actual <= lastAppliedDelay * 3)
        }
    }

    @Test
    fun `does not overflow to infinite delay when three times last applied delay would be infinite`() {
        val initialDelay = 100.milliseconds
        val maxDelay = 1.seconds
        val lastAppliedDelay = ((Long.MAX_VALUE / 2) - 1).milliseconds

        assertTrue(lastAppliedDelay.isFinite())
        assertTrue((lastAppliedDelay * 3).isInfinite())

        val backoff = DecorrelatedBackoff(initialDelay = initialDelay, maxDelay = maxDelay)

        val backoffContext = BackoffContext(attempt = 2, lastAppliedDelay = lastAppliedDelay)
        val actual = backoff.nextDelay(backoffContext)

        assertTrue(actual >= initialDelay)
        assertTrue(actual <= maxDelay)
        assertTrue(actual.isFinite())
    }

    @Test
    fun `throws IllegalArgumentException for negative initial delay`() {
        assertFailsWith<IllegalArgumentException> {
            DecorrelatedBackoff(initialDelay = (-100).milliseconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException for infinite initial delay`() {
        assertFailsWith<IllegalArgumentException> {
            DecorrelatedBackoff(initialDelay = Duration.INFINITE)
        }
    }

    @Test
    fun `throws IllegalArgumentException for zero max delay`() {
        assertFailsWith<IllegalArgumentException> {
            DecorrelatedBackoff(initialDelay = 100.milliseconds, maxDelay = Duration.ZERO)
        }
    }

    @Test
    fun `throws IllegalArgumentException for negative max delay`() {
        assertFailsWith<IllegalArgumentException> {
            DecorrelatedBackoff(initialDelay = 100.milliseconds, maxDelay = (-1).milliseconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException when max delay is less than initial delay`() {
        assertFailsWith<IllegalArgumentException> {
            DecorrelatedBackoff(initialDelay = 100.milliseconds, maxDelay = 99.milliseconds)
        }
    }
}
