package io.github.straxess.retrykt

import io.github.straxess.retrykt.backoff.Backoff
import io.github.straxess.retrykt.backoff.BackoffContext
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
    fun `onRetryAttempt receives nextDelay`() {
        val nextDelays = mutableListOf<Duration>()

        retryBlocking(
            onRetryAttempt = { nextDelays += it.plan.nextDelay },
            backoff = object : Backoff {
                override fun nextDelay(context: BackoffContext) = 0.milliseconds * context.attempt
            },
            jitter = { rawDelay -> rawDelay + 0.milliseconds },
        ) {
            if (it.attempt < 3) {
                throw IllegalStateException()
            }
        }

        assertEquals(listOf(0.milliseconds, 0.milliseconds), nextDelays)
    }

    @Test
    fun `jitter receives raw delay from backoff`() {
        val rawDelays = mutableListOf<Duration>()

        retryBlocking(
            maxAttempts = 2,
            backoff = object : Backoff {
                override fun nextDelay(context: BackoffContext) = 0.milliseconds
            },
            jitter = {
                rawDelays += it
                it
            },
        ) {
            if (it.attempt == 1) {
                throw IllegalStateException()
            }
        }

        assertEquals(listOf(0.milliseconds), rawDelays)
    }

    @Test
    fun `backoff receives last applied delay`() {
        val lastAppliedDelays = mutableListOf<Duration?>()

        retryBlocking(
            backoff = object : Backoff {
                override fun nextDelay(context: BackoffContext): Duration {
                    lastAppliedDelays += context.lastAppliedDelay
                    return 0.milliseconds * context.attempt
                }
            },
            jitter = { it + 0.milliseconds },
        ) {
            if (it.attempt < 4) {
                throw IllegalStateException()
            }
        }

        assertEquals(listOf(null, 0.milliseconds, 0.milliseconds), lastAppliedDelays)
    }
}
