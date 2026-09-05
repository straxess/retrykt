package io.github.straxess.retrykt.backoff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
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
    fun `caps delay when linear value exceeds max delay`() {
        val backoff = LinearBackoff(
            increment = 100.milliseconds,
            maxDelay = 950.milliseconds,
        )

        assertEquals(900.milliseconds, backoff.nextDelay(BackoffContext(9, null)))
        assertEquals(950.milliseconds, backoff.nextDelay(BackoffContext(10, null)))
        assertEquals(950.milliseconds, backoff.nextDelay(BackoffContext(11, null)))
    }

    @Test
    fun `does not cap delay when max delay is infinite`() {
        val backoff = LinearBackoff(
            increment = 100.milliseconds,
            maxDelay = Duration.INFINITE,
        )

        assertEquals(200.milliseconds, backoff.nextDelay(BackoffContext(2, null)))
        assertEquals(10.seconds, backoff.nextDelay(BackoffContext(100, null)))
    }

    @Test
    fun `returns zero when increment is zero`() {
        val backoff = LinearBackoff(
            increment = Duration.ZERO,
            maxDelay = 10.seconds,
        )

        val actual = backoff.nextDelay(
            BackoffContext(
                attempt = Int.MAX_VALUE,
                lastAppliedDelay = null,
            ),
        )

        assertEquals(Duration.ZERO, actual)
    }

    @Test
    fun `returns max delay when increment equals max delay`() {
        val backoff = LinearBackoff(
            increment = 1.seconds,
            maxDelay = 1.seconds,
        )

        assertEquals(1.seconds, backoff.nextDelay(BackoffContext(1, null)))
        assertEquals(1.seconds, backoff.nextDelay(BackoffContext(2, null)))
        assertEquals(1.seconds, backoff.nextDelay(BackoffContext(100, null)))
    }

    @Test
    fun `does not overflow for max attempt`() {
        val maxDelay = 10.days

        val backoff = LinearBackoff(
            increment = 1.days,
            maxDelay = maxDelay,
        )

        val actual = backoff.nextDelay(
            BackoffContext(
                attempt = Int.MAX_VALUE,
                lastAppliedDelay = null,
            ),
        )

        assertEquals(maxDelay, actual)
    }

    @Test
    fun `throws IllegalArgumentException if increment is less than 0`() {
        assertFailsWith<IllegalArgumentException> {
            LinearBackoff((-10).seconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException if infinite increment`() {
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
