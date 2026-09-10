package io.github.straxess.retrykt.backoff

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class DecorrelatedBackoffTest {

    @Test
    fun `returns zero when initial delay is zero`() {
        val backoff =
            DecorrelatedBackoff(
                initialDelay = Duration.ZERO,
                maxDelay = Duration.ZERO,
            )
        var prevAppliedDelay: Duration? = null

        repeat(100) {
            val backoffContext = BackoffContext(attempt = 2, prevAppliedDelay = prevAppliedDelay)
            val actual = backoff.nextDelay(backoffContext)

            assertEquals(actual, Duration.ZERO)

            prevAppliedDelay = actual
        }
    }

    @Test
    fun `returns initial delay on first attempt`() {
        val backoff = DecorrelatedBackoff(initialDelay = 100.milliseconds, maxDelay = 10.seconds)

        val actual =
            backoff.nextDelay(
                BackoffContext(attempt = 1, prevAppliedDelay = null),
            )

        assertEquals(100.milliseconds, actual)
    }

    @Test
    fun `returns delay between initial delay and three times prev applied delay`() {
        val initialDelay = 100.milliseconds
        val prevAppliedDelay = 200.milliseconds

        val backoff = DecorrelatedBackoff(initialDelay = initialDelay, maxDelay = 10.seconds)
        val random = Random(0)

        repeat(100) {
            val actual =
                backoff.nextDelay(
                    BackoffContext(attempt = 2, prevAppliedDelay = prevAppliedDelay),
                    random,
                )

            assertTrue(actual >= initialDelay)
            assertTrue(actual <= prevAppliedDelay * 3)
        }
    }

    @Test
    fun `returns randomized delay`() {
        val initialDelay = 100.milliseconds
        val prevAppliedDelay = 200.milliseconds
        val maxDelay = 10.seconds

        val backoff = DecorrelatedBackoff(initialDelay = initialDelay, maxDelay = maxDelay)
        val random = Random(0)

        val delays =
            buildSet {
                repeat(100) {
                    val backoffContext = BackoffContext(attempt = 2, prevAppliedDelay = prevAppliedDelay)
                    add(backoff.nextDelay(backoffContext, random))
                }
            }

        assertTrue(delays.size > 1)
    }

    @Test
    fun `returns initial delay when prev applied delay is below initial delay`() {
        val initialDelay = 100.milliseconds

        val backoff = DecorrelatedBackoff(initialDelay = initialDelay, maxDelay = 10.seconds)

        val actual =
            backoff.nextDelay(
                BackoffContext(attempt = 2, prevAppliedDelay = 20.milliseconds),
            )

        assertEquals(initialDelay, actual)
    }

    @Test
    fun `returns initial delay when max delay equals initial delay`() {
        val delay = 100.milliseconds

        val backoff = DecorrelatedBackoff(initialDelay = delay, maxDelay = delay)

        repeat(100) {
            val backoffContext = BackoffContext(attempt = 2, prevAppliedDelay = 200.milliseconds)
            val actual = backoff.nextDelay(backoffContext)

            assertEquals(delay, actual)
        }
    }

    @Test
    fun `returns randomized delay up to max delay when three times prev applied delay exceeds max delay`() {
        val initialDelay = 100.milliseconds
        val maxDelay = 500.milliseconds
        val prevAppliedDelay = 200.milliseconds

        val backoff = DecorrelatedBackoff(initialDelay = initialDelay, maxDelay = maxDelay)
        val random = Random(0)

        repeat(100) {
            val backoffContext = BackoffContext(attempt = 2, prevAppliedDelay = prevAppliedDelay)
            val actual = backoff.nextDelay(backoffContext, random)

            assertTrue(actual >= initialDelay)
            assertTrue(actual <= maxDelay)
        }
    }

    @Test
    fun `returns randomized delay up to three times prev applied delay`() {
        val initialDelay = 100.milliseconds
        val prevAppliedDelay = 200.milliseconds

        val backoff =
            DecorrelatedBackoff(
                initialDelay = initialDelay,
                maxDelay = 1.days,
            )
        val random = Random(0)

        repeat(100) {
            val backoffContext = BackoffContext(attempt = 2, prevAppliedDelay = prevAppliedDelay)
            val actual = backoff.nextDelay(backoffContext, random)

            assertTrue(actual >= initialDelay)
            assertTrue(actual <= prevAppliedDelay * 3)
        }
    }

    @Test
    fun `does not overflow to infinite delay when three times prev applied delay would be infinite`() {
        val initialDelay = 100.milliseconds
        val maxDelay = 1.seconds
        val prevAppliedDelay = ((Long.MAX_VALUE / 2) - 1).milliseconds

        assertTrue(prevAppliedDelay.isFinite())
        assertTrue((prevAppliedDelay * 3).isInfinite())

        val backoff = DecorrelatedBackoff(initialDelay = initialDelay, maxDelay = maxDelay)
        val random = Random(0)

        val delays =
            List(1_000) {
                backoff.nextDelay(
                    BackoffContext(attempt = 2, prevAppliedDelay = prevAppliedDelay),
                    random,
                )
            }

        assertTrue(delays.all { it >= initialDelay })
        assertTrue(delays.all { it <= maxDelay })
        assertTrue(delays.all { it.isFinite() })
        assertTrue(delays.toSet().size > 1)
        assertTrue(delays.count { it == maxDelay } < 10)
    }

    @Test
    fun `throws IllegalArgumentException for negative initial delay`() {
        assertFailsWith<IllegalArgumentException> {
            DecorrelatedBackoff(initialDelay = (-100).milliseconds, maxDelay = 100.milliseconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException for infinite initial delay`() {
        assertFailsWith<IllegalArgumentException> {
            DecorrelatedBackoff(initialDelay = Duration.INFINITE, maxDelay = 100.milliseconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException for negative max delay`() {
        assertFailsWith<IllegalArgumentException> {
            DecorrelatedBackoff(initialDelay = 100.milliseconds, maxDelay = (-1).milliseconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException for infinite max delay`() {
        assertFailsWith<IllegalArgumentException> {
            DecorrelatedBackoff(initialDelay = 100.milliseconds, maxDelay = Duration.INFINITE)
        }
    }

    @Test
    fun `throws IllegalArgumentException when max delay is less than initial delay`() {
        assertFailsWith<IllegalArgumentException> {
            DecorrelatedBackoff(initialDelay = 100.milliseconds, maxDelay = 99.milliseconds)
        }
    }
}
