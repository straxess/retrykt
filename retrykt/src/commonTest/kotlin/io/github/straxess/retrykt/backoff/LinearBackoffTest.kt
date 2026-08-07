package io.github.straxess.retrykt.backoff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
    fun `LinearBackoff throws IllegalArgumentException if increment is less than 0`() {
        assertFailsWith<IllegalArgumentException> {
            LinearBackoff((-10).seconds)
        }
    }
}
