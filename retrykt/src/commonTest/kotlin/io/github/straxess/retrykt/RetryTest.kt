package io.github.straxess.retrykt

import io.github.straxess.retrykt.backoff.Backoff
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class RetryTest {

    @Test
    fun `does not retry successful result by default`() = runTest {
        var attempts = 0

        val result = retry {
            attempts++
            "success"
        }

        assertEquals("success", result)
        assertEquals(1, attempts)
    }

    @Test
    fun `success after some failures`() = runTest {
        var attempts = 0

        retry {
            attempts++

            if (attempts < 5) {
                throw RuntimeException()
            }
        }

        assertEquals(5, attempts)
    }

    @Test
    fun `stops with RetryStoppedException when max attempts reached`() = runTest {
        var attempts = 0

        val exception = assertFailsWith<RetryStoppedException> {
            retry(maxAttempts = 3) {
                attempts++
                throw RuntimeException()
            }
        }

        assertEquals(3, attempts)
        assertTrue(exception.reason is RetryStoppedReason.MaxAttemptsReached)
        assertEquals(3, exception.reason.maxAttempts)
    }

    @Test
    fun `does not retry when throwable does not match retryOn`() = runTest {
        var attempts = 0

        assertFailsWith<IllegalStateException> {
            retry(retryOn = RetryOn.thrown { it is IllegalArgumentException }) {
                attempts++
                throw IllegalStateException()
            }
        }

        assertEquals(1, attempts)
    }

    @Test
    fun `retries when throwable matches retryOn`() = runTest {
        var attempts = 0

        retry(retryOn = RetryOn.thrown { it is IllegalStateException }) {
            attempts++
            if (attempts == 1) {
                throw IllegalStateException()
            }
        }

        assertEquals(2, attempts)
    }

    @Test
    fun `always rethrows CancellationException`() = runTest {
        assertFailsWith<CancellationException> {
            retry(retryOn = RetryOn.outcome { error("should not be called") }) {
                throw CancellationException()
            }
        }
    }

    @Test
    fun `stops retrying when throwable no longer matches retryOn`() = runTest {
        var attempts = 0

        assertFailsWith<IllegalArgumentException> {
            retry(retryOn = RetryOn.thrown { it is IllegalStateException }) {
                attempts++

                if (attempts == 1) {
                    throw IllegalStateException()
                }

                throw IllegalArgumentException()
            }
        }

        assertEquals(2, attempts)
    }

    @Test
    fun `retries when returned result matches retryOn`() = runTest {
        var attempts = 0

        val result = retry(retryOn = RetryOn.returned { it == "retry" }) {
            attempts++

            if (attempts < 3) {
                "retry"
            } else {
                "success"
            }
        }

        assertEquals("success", result)
        assertEquals(3, attempts)
    }

    @Test
    fun `returns result when returned result does not match retryOn`() = runTest {
        var attempts = 0

        val result = retry(retryOn = RetryOn.returned { it == "retry" }) {
            attempts++
            "success"
        }

        assertEquals("success", result)
        assertEquals(1, attempts)
    }

    @Test
    fun `attempt index increments on each retry`() = runTest {
        val attempts = mutableListOf<Int>()

        retry(maxAttempts = 4) {
            attempts += it.attempt

            if (it.attempt < 4) {
                throw RuntimeException()
            }
        }

        assertEquals(listOf(1, 2, 3, 4), attempts)
    }

    @Test
    fun `retry context maxAttempts matches configured maxAttempts`() = runTest {
        val maxAttempts = 3
        val attempts = mutableListOf<Int>()

        assertFailsWith<RetryStoppedException> {
            retry(maxAttempts = maxAttempts) { context ->
                attempts += context.maxAttempts
                throw RuntimeException()
            }
        }

        assertEquals(listOf(3, 3, 3), attempts)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `retry delays between attempts`() = runTest {
        var attempts = 0

        retry(
            maxAttempts = 2,
            backoff = object : Backoff {
                override fun nextDelay(attempt: Int) = 20.milliseconds
            }
        ) {
            attempts++
            if (attempts == 1) {
                throw RuntimeException("fail")
            }
        }

        assertEquals(2, attempts)
        assertEquals(20, currentTime)
    }

    @Test
    fun `retry throws IllegalArgumentException if maxAttempts is less than 1`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            retry(maxAttempts = 0) {}
        }
    }

    @Test
    fun `onRetryAttempt receives current thrown outcome`() = runTest {
        val first = IllegalStateException()
        val second = IllegalArgumentException()

        val outcomes = mutableListOf<AttemptOutcome<Unit>>()

        retry(onRetryAttempt = { outcomes += it.outcome }) {
            when (it.attempt) {
                1 -> throw first
                2 -> throw second
                else -> {}
            }
        }

        assertEquals(2, outcomes.size)
        assertTrue(outcomes[0] is AttemptOutcome.Thrown)
        assertTrue(outcomes[1] is AttemptOutcome.Thrown)
        assertSame(first, (outcomes[0] as AttemptOutcome.Thrown).throwable)
        assertSame(second, (outcomes[1] as AttemptOutcome.Thrown).throwable)
    }

    @Test
    fun `onRetryAttempt receives returned outcome`() = runTest {
        val outcomes = mutableListOf<AttemptOutcome<String>>()

        retry(
            retryOn = RetryOn.returned { it == "retry" },
            onRetryAttempt = { outcomes += it.outcome },
        ) {
            if (it.attempt < 2) {
                "retry"
            } else {
                "success"
            }
        }

        assertEquals(1, outcomes.size)

        assertTrue(outcomes[0] is AttemptOutcome.Returned)
        assertEquals("retry", (outcomes[0] as AttemptOutcome.Returned).value)
    }

    @Test
    fun `onRetryAttempt receives current attempt`() = runTest {
        val attempts = mutableListOf<Int>()

        retry(onRetryAttempt = { attempts += it.context.attempt }) {
            if (it.attempt < 3) {
                throw IllegalStateException()
            }
        }

        assertEquals(listOf(1, 2), attempts)
    }

    @Test
    fun `onRetryAttempt receives nextDelay`() = runTest {
        val nextDelays = mutableListOf<Duration>()

        retry(
            onRetryAttempt = { nextDelays += it.plan.nextDelay },
            backoff = object : Backoff {
                override fun nextDelay(attempt: Int) = 100.milliseconds * attempt
            }
        ) {
            if (it.attempt < 3) {
                throw IllegalStateException()
            }
        }

        assertEquals(listOf(100.milliseconds, 200.milliseconds), nextDelays)
    }

    @Test
    fun `onRetryAttempt is called before next attempt`() = runTest {
        val events = mutableListOf<String>()

        retry(onRetryAttempt = { events += "retry-${it.context.attempt}" }) {
            events += "attempt-${it.attempt}"

            if (it.attempt < 3) {
                throw IllegalStateException()
            }
        }

        assertEquals(
            listOf("attempt-1", "retry-1", "attempt-2", "retry-2", "attempt-3"),
            events
        )
    }
}
