package io.github.straxess.retrykt

import io.github.straxess.retrykt.backoff.Backoff
import io.github.straxess.retrykt.backoff.ConstantBackoff
import io.github.straxess.retrykt.backoff.NoBackoff
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RetryBlockingWasmJsTest {

    @Test
    fun `retryBlocking with NoBackoff works`() {
        var attempts = 0

        val result = retryBlocking(backoff = NoBackoff) {
            attempts++

            if (attempts < 3) {
                error("fail")
            }

            "success"
        }

        assertEquals("success", result)
        assertEquals(3, attempts)
    }

    @Test
    fun `retryBlocking with Backoff fails`() {
        assertFailsWith<UnsupportedOperationException> {
            retryBlocking(backoff = ConstantBackoff(1.seconds)) {
                error("fail")
            }
        }
    }

    @Test
    fun `onRetry receives nextDelay`() {
        val nextDelays = mutableListOf<Duration>()

        retryBlocking(
            onRetry = { nextDelays += it.plan.nextDelay },
            backoff = object : Backoff {
                override fun nextDelay(attempt: Int) = 0.milliseconds * attempt
            }
        ) {
            if (it.attempt < 3) {
                throw IllegalStateException()
            }
        }

        assertEquals(listOf(0.milliseconds, 0.milliseconds), nextDelays)
    }
}
