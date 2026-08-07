package io.github.straxess.retrykt

import io.github.straxess.retrykt.backoff.Backoff
import io.github.straxess.retrykt.backoff.BackoffContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class RetryBlockingJvmTest {

    @Test
    fun `retry blocks between attempts`() {
        val start = TimeSource.Monotonic.markNow()

        var attempts = 0

        retryBlocking(
            maxAttempts = 2,
            backoff = object : Backoff {
                override fun nextDelay(context: BackoffContext) = 20.milliseconds
            },
            jitter = { rawDelay -> rawDelay + 10.milliseconds },
        ) {
            attempts++

            if (attempts == 1) {
                throw RuntimeException()
            }
        }

        assertEquals(2, attempts)
        assertTrue(start.elapsedNow() >= 30.milliseconds)
    }

    @Test
    fun `onRetryAttempt receives nextDelay`() {
        val nextDelays = mutableListOf<Duration>()

        retryBlocking(
            onRetryAttempt = { nextDelays += it.plan.nextDelay },
            backoff = object : Backoff {
                override fun nextDelay(context: BackoffContext) = 10.milliseconds * context.attempt
            },
            jitter = { rawDelay -> rawDelay + 10.milliseconds },
        ) {
            if (it.attempt < 3) {
                throw IllegalStateException()
            }
        }

        assertEquals(listOf(20.milliseconds, 30.milliseconds), nextDelays)
    }

    @Test
    fun `jitter receives raw delay from backoff`() {
        val rawDelays = mutableListOf<Duration>()

        retryBlocking(
            maxAttempts = 2,
            backoff = object : Backoff {
                override fun nextDelay(context: BackoffContext) = 100.milliseconds
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

        assertEquals(listOf(100.milliseconds), rawDelays)
    }

    @Test
    fun `backoff receives last applied delay`() {
        val lastAppliedDelays = mutableListOf<Duration?>()

        retryBlocking(
            backoff = object : Backoff {
                override fun nextDelay(context: BackoffContext): Duration {
                    lastAppliedDelays += context.lastAppliedDelay
                    return 100.milliseconds * context.attempt
                }
            },
            jitter = { it + 50.milliseconds },
        ) {
            if (it.attempt < 4) {
                throw IllegalStateException()
            }
        }

        assertEquals(listOf(null, 150.milliseconds, 250.milliseconds), lastAppliedDelays)
    }
}
