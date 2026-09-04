package io.github.straxess.retrykt.backoff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LinearBackoffTest {

    @Test
    fun `returns linear delay`() {
        val backoff = LinearBackoff(10.seconds)

        val firstDelay = backoff.nextDelay(BackoffContext(1, null))
        val secondDelay = backoff.nextDelay(BackoffContext(2, null))
        val thirdDelay = backoff.nextDelay(BackoffContext(3, null))

        assertEquals(10.seconds, firstDelay)
        assertEquals(20.seconds, secondDelay)
        assertEquals(30.seconds, thirdDelay)
    }

    @Test
    fun `caps delay at max delay`() {
        val increment = 100.milliseconds
        val lastAppliedDelay = 10.seconds
        val maxDelay = 1.seconds

        val backoff = LinearBackoff(increment = increment, maxDelay = maxDelay)

        repeat(100) {
            val actual = backoff.nextDelay(
                BackoffContext(attempt = 2, lastAppliedDelay = lastAppliedDelay),
            )

            assertTrue(actual >= increment)
            assertTrue(actual <= maxDelay)
        }
    }

    @Test
    fun `accepts infinite max delay`() {
        val backoff = LinearBackoff(increment = 100.milliseconds, maxDelay = Duration.INFINITE)

        val actual = backoff.nextDelay(
            BackoffContext(attempt = 2, lastAppliedDelay = 200.milliseconds),
        )

        assertTrue(actual >= 100.milliseconds)
    }

    @Test
    fun `throws IllegalArgumentException if increment is less than 0`() {
        assertFailsWith<IllegalArgumentException> {
            LinearBackoff((-10).seconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException if infinite constant delay`() {
        assertFailsWith<IllegalArgumentException> {
            LinearBackoff(Duration.INFINITE)
        }
    }

    @Test
    fun `throws IllegalArgumentException when max delay is less than increment`() {
        assertFailsWith<IllegalArgumentException> {
            LinearBackoff(increment = 100.milliseconds, maxDelay = 99.milliseconds)
        }
    }
}
