package io.github.straxess.retrykt

import io.github.straxess.retrykt.backoff.Backoff
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class RetryBlockingAndroidHostTest {

    @Test
    fun `retry blocks between attempts`() {
        val start = TimeSource.Monotonic.markNow()

        var attempts = 0

        retryBlocking(
            maxAttempts = 2,
            backoff = object : Backoff {
                override fun nextDelay(attempt: Int) = 20.milliseconds
            }
        ) {
            attempts++

            if (attempts == 1) {
                throw RuntimeException()
            }
        }

        assertEquals(2, attempts)
        assertTrue(start.elapsedNow() >= 20.milliseconds)
    }
}
