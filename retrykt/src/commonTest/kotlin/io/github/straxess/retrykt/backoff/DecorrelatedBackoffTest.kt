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
    fun `returns zero when first delay is zero`() {
        val backoff =
            DecorrelatedBackoff(
                firstDelay = Duration.ZERO,
                maxDelay = Duration.ZERO,
            )
        var prevAppliedDelay: Duration? = null

        repeat(100) {
            val backoffContext = BackoffContext(attempt = 2, prevAppliedDelay = prevAppliedDelay)
            val actual = backoff.calculateDelay(backoffContext)

            assertEquals(actual, Duration.ZERO)

            prevAppliedDelay = actual
        }
    }

    @Test
    fun `returns first delay after first attempt`() {
        val backoff = DecorrelatedBackoff(firstDelay = 100.milliseconds, maxDelay = 10.seconds)

        val actual =
            backoff.calculateDelay(
                BackoffContext(attempt = 1, prevAppliedDelay = null),
            )

        assertEquals(100.milliseconds, actual)
    }

    @Test
    fun `returns delay between first delay and three times prev applied delay`() {
        val firstDelay = 100.milliseconds
        val prevAppliedDelay = 200.milliseconds

        val backoff = DecorrelatedBackoff(firstDelay = firstDelay, maxDelay = 10.seconds)
        val random = Random(0)

        repeat(100) {
            val actual =
                backoff.calculateDelay(
                    BackoffContext(attempt = 2, prevAppliedDelay = prevAppliedDelay),
                    random,
                )

            assertTrue(actual >= firstDelay)
            assertTrue(actual <= prevAppliedDelay * 3)
        }
    }

    @Test
    fun `returns randomized delay`() {
        val firstDelay = 100.milliseconds
        val prevAppliedDelay = 200.milliseconds
        val maxDelay = 10.seconds

        val backoff = DecorrelatedBackoff(firstDelay = firstDelay, maxDelay = maxDelay)
        val random = Random(0)

        val delays =
            buildSet {
                repeat(100) {
                    val backoffContext = BackoffContext(attempt = 2, prevAppliedDelay = prevAppliedDelay)
                    add(backoff.calculateDelay(backoffContext, random))
                }
            }

        assertTrue(delays.size > 1)
    }

    @Test
    fun `returns first delay when prev applied delay is below first delay`() {
        val firstDelay = 100.milliseconds

        val backoff = DecorrelatedBackoff(firstDelay = firstDelay, maxDelay = 10.seconds)

        val actual =
            backoff.calculateDelay(
                BackoffContext(attempt = 2, prevAppliedDelay = 20.milliseconds),
            )

        assertEquals(firstDelay, actual)
    }

    @Test
    fun `returns first delay when max delay equals first delay`() {
        val delay = 100.milliseconds

        val backoff = DecorrelatedBackoff(firstDelay = delay, maxDelay = delay)

        repeat(100) {
            val backoffContext = BackoffContext(attempt = 2, prevAppliedDelay = 200.milliseconds)
            val actual = backoff.calculateDelay(backoffContext)

            assertEquals(delay, actual)
        }
    }

    @Test
    fun `returns randomized delay up to max delay when three times prev applied delay exceeds max delay`() {
        val firstDelay = 100.milliseconds
        val maxDelay = 500.milliseconds
        val prevAppliedDelay = 200.milliseconds

        val backoff = DecorrelatedBackoff(firstDelay = firstDelay, maxDelay = maxDelay)
        val random = Random(0)

        repeat(100) {
            val backoffContext = BackoffContext(attempt = 2, prevAppliedDelay = prevAppliedDelay)
            val actual = backoff.calculateDelay(backoffContext, random)

            assertTrue(actual >= firstDelay)
            assertTrue(actual <= maxDelay)
        }
    }

    @Test
    fun `returns randomized delay up to three times prev applied delay`() {
        val firstDelay = 100.milliseconds
        val prevAppliedDelay = 200.milliseconds

        val backoff =
            DecorrelatedBackoff(
                firstDelay = firstDelay,
                maxDelay = 1.days,
            )
        val random = Random(0)

        repeat(100) {
            val backoffContext = BackoffContext(attempt = 2, prevAppliedDelay = prevAppliedDelay)
            val actual = backoff.calculateDelay(backoffContext, random)

            assertTrue(actual >= firstDelay)
            assertTrue(actual <= prevAppliedDelay * 3)
        }
    }

    @Test
    fun `does not overflow to infinite delay when three times prev applied delay would be infinite`() {
        val firstDelay = 100.milliseconds
        val maxDelay = 1.seconds
        val prevAppliedDelay = ((Long.MAX_VALUE / 2) - 1).milliseconds

        assertTrue(prevAppliedDelay.isFinite())
        assertTrue((prevAppliedDelay * 3).isInfinite())

        val backoff = DecorrelatedBackoff(firstDelay = firstDelay, maxDelay = maxDelay)
        val random = Random(0)

        val delays =
            List(1_000) {
                backoff.calculateDelay(
                    BackoffContext(attempt = 2, prevAppliedDelay = prevAppliedDelay),
                    random,
                )
            }

        assertTrue(delays.all { it >= firstDelay })
        assertTrue(delays.all { it <= maxDelay })
        assertTrue(delays.all { it.isFinite() })
        assertTrue(delays.toSet().size > 1)
        assertTrue(delays.count { it == maxDelay } < 10)
    }

    @Test
    fun `throws IllegalArgumentException for negative first delay`() {
        assertFailsWith<IllegalArgumentException> {
            DecorrelatedBackoff(firstDelay = (-100).milliseconds, maxDelay = 100.milliseconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException for infinite first delay`() {
        assertFailsWith<IllegalArgumentException> {
            DecorrelatedBackoff(firstDelay = Duration.INFINITE, maxDelay = 100.milliseconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException for negative max delay`() {
        assertFailsWith<IllegalArgumentException> {
            DecorrelatedBackoff(firstDelay = 100.milliseconds, maxDelay = (-1).milliseconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException for infinite max delay`() {
        assertFailsWith<IllegalArgumentException> {
            DecorrelatedBackoff(firstDelay = 100.milliseconds, maxDelay = Duration.INFINITE)
        }
    }

    @Test
    fun `throws IllegalArgumentException when max delay is less than first delay`() {
        assertFailsWith<IllegalArgumentException> {
            DecorrelatedBackoff(firstDelay = 100.milliseconds, maxDelay = 99.milliseconds)
        }
    }
}
