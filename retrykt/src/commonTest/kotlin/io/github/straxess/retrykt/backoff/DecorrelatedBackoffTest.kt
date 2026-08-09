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
    fun `caps delay at max delay`() {
        val initialDelay = 100.milliseconds
        val lastAppliedDelay = 10.seconds
        val maxDelay = 1.seconds

        val backoff = DecorrelatedBackoff(initialDelay = initialDelay, maxDelay = maxDelay)

        repeat(100) {
            val actual = backoff.nextDelay(
                BackoffContext(attempt = 2, lastAppliedDelay = lastAppliedDelay),
            )

            assertTrue(actual >= initialDelay)
            assertTrue(actual <= maxDelay)
        }
    }

    @Test
    fun `returns initial delay when last applied delay is too small`() {
        val initialDelay = 100.milliseconds

        val backoff = DecorrelatedBackoff(initialDelay = initialDelay, maxDelay = 10.seconds)

        val actual = backoff.nextDelay(
            BackoffContext(attempt = 2, lastAppliedDelay = 20.milliseconds),
        )

        assertEquals(initialDelay, actual)
    }

    @Test
    fun `returns randomized delay`() {
        val initialDelay = 100.milliseconds

        val backoff = DecorrelatedBackoff(initialDelay = initialDelay, maxDelay = 10.seconds)

        var lastAppliedDelay: Duration? = null
        val delays = buildSet {
            repeat(100) {
                val actual = backoff.nextDelay(
                    BackoffContext(attempt = it + 1, lastAppliedDelay = lastAppliedDelay),
                )
                lastAppliedDelay = actual
                add(actual)
            }
        }

        assertTrue(delays.size > 1)
    }

    @Test
    fun `returns initial delay when last applied delay is zero`() {
        val initialDelay = 100.milliseconds

        val backoff = DecorrelatedBackoff(initialDelay = initialDelay, maxDelay = 10.seconds)

        val actual = backoff.nextDelay(
            BackoffContext(attempt = 2, lastAppliedDelay = Duration.ZERO),
        )

        assertEquals(initialDelay, actual)
    }

    @Test
    fun `accepts infinite max delay`() {
        val backoff = DecorrelatedBackoff(initialDelay = 100.milliseconds, maxDelay = Duration.INFINITE)

        repeat(100) {
            val actual = backoff.nextDelay(
                BackoffContext(attempt = 2, lastAppliedDelay = 200.milliseconds),
            )

            assertTrue(actual >= 100.milliseconds)
            assertTrue(actual <= 600.milliseconds)
        }
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
}
