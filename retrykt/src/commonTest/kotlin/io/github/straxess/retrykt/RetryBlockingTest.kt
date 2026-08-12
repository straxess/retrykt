package io.github.straxess.retrykt

import io.github.straxess.retrykt.backoff.Backoff
import io.github.straxess.retrykt.backoff.BackoffContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class RetryBlockingTest {

    @Test
    fun `does not retry successful result by default`() {
        var attempts = 0

        val result = retryBlocking {
            attempts++
            "success"
        }

        assertEquals("success", result)
        assertEquals(1, attempts)
    }

    @Test
    fun `success after some failures`() {
        var attempts = 0

        retryBlocking {
            attempts++

            if (attempts < 5) {
                throw RuntimeException()
            }
        }

        assertEquals(5, attempts)
    }

    @Test
    fun `stops with RetryStoppedException when max attempts reached with thrown outcome`() {
        val maxAttempts = 3
        val expectedThrowable = RuntimeException()
        var attempts = 0

        val exception = assertFailsWith<RetryStoppedException> {
            retryBlocking(maxAttempts = maxAttempts) {
                attempts++
                throw expectedThrowable
            }
        }

        assertEquals(3, attempts)
        assertTrue(exception.reason is RetryStoppedReason.MaxAttemptsReached)
        assertEquals(maxAttempts, exception.reason.maxAttempts)
        assertTrue(exception.lastOutcome is AttemptOutcome.Thrown)
        assertSame(expectedThrowable, exception.lastOutcome.throwable)
        assertSame(expectedThrowable, exception.cause)
    }

    @Test
    fun `stops with RetryStoppedException when max attempts reached with returned outcome`() {
        val maxAttempts = 3
        val expectedReturned = 1
        var attempts = 0

        val exception = assertFailsWith<RetryStoppedException> {
            retryBlocking(maxAttempts = maxAttempts, retryOn = RetryOn.returned { it == 1 }) {
                attempts++
                expectedReturned
            }
        }

        assertEquals(3, attempts)
        assertTrue(exception.reason is RetryStoppedReason.MaxAttemptsReached)
        assertEquals(maxAttempts, exception.reason.maxAttempts)
        assertTrue(exception.lastOutcome is AttemptOutcome.Returned)
        assertEquals(expectedReturned, exception.lastOutcome.value)
    }

    @Test
    fun `does not retry when throwable does not match retryOn`() {
        var attempts = 0

        assertFailsWith<IllegalStateException> {
            retryBlocking(retryOn = RetryOn.thrown { it is IllegalArgumentException }) {
                attempts++
                throw IllegalStateException()
            }
        }

        assertEquals(1, attempts)
    }

    @Test
    fun `retries when throwable matches retryOn`() {
        var attempts = 0

        retryBlocking(retryOn = RetryOn.thrown { it is IllegalStateException }) {
            attempts++
            if (attempts == 1) {
                throw IllegalStateException()
            }
        }

        assertEquals(2, attempts)
    }

    @Test
    fun `always rethrows CancellationException`() {
        assertFailsWith<CancellationException> {
            retryBlocking(retryOn = RetryOn.outcome { error("should not be called") }) {
                throw CancellationException()
            }
        }
    }

    @Test
    fun `rejects invalid custom delays`() {
        assertFailsWith<IllegalArgumentException> {
            retryBlocking(backoff = object : Backoff {
                override fun nextDelay(context: BackoffContext) = (-1).milliseconds
            }) {
                error("task should not succeed")
            }
        }

        assertFailsWith<IllegalArgumentException> {
            retryBlocking(jitter = { Duration.INFINITE }) {
                error("task should not succeed")
            }
        }
    }

    @Test
    fun `stops retrying when throwable no longer matches retryOn`() {
        var attempts = 0

        assertFailsWith<IllegalArgumentException> {
            retryBlocking(retryOn = RetryOn.thrown { it is IllegalStateException }) {
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
    fun `retries when returned result matches retryOn`() {
        var attempts = 0

        val result = retryBlocking(retryOn = RetryOn.returned { it == "retry" }) {
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
    fun `returns result when returned result does not match retryOn`() {
        var attempts = 0

        val result = retryBlocking(retryOn = RetryOn.returned { it == "retry" }) {
            attempts++
            "success"
        }

        assertEquals("success", result)
        assertEquals(1, attempts)
    }

    @Test
    fun `attempt index increments on each retry`() {
        val attempts = mutableListOf<Int>()

        retryBlocking(maxAttempts = 4) {
            attempts += it.attempt

            if (it.attempt < 4) {
                throw RuntimeException()
            }
        }

        assertEquals(listOf(1, 2, 3, 4), attempts)
    }

    @Test
    fun `retry context maxAttempts matches configured maxAttempts`() {
        val maxAttempts = 3
        val attempts = mutableListOf<Int>()

        assertFailsWith<RetryStoppedException> {
            retryBlocking(maxAttempts = maxAttempts) { context ->
                attempts += context.maxAttempts
                throw RuntimeException()
            }
        }

        assertEquals(listOf(3, 3, 3), attempts)
    }

    @Test
    fun `retry throws IllegalArgumentException if maxAttempts is less than 1`() {
        assertFailsWith<IllegalArgumentException> {
            retryBlocking(maxAttempts = 0) {}
        }
    }

    @Test
    fun `onRetryAttempt receives current thrown outcome`() {
        val first = IllegalStateException()
        val second = IllegalArgumentException()

        val outcomes = mutableListOf<AttemptOutcome<Unit>>()

        retryBlocking(onRetryAttempt = { outcomes += it.outcome }) {
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
    fun `onRetryAttempt receives returned outcome`() {
        val outcomes = mutableListOf<AttemptOutcome<String>>()

        retryBlocking(
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
    fun `onRetryAttempt receives current attempt`() {
        val attempts = mutableListOf<Int>()

        retryBlocking(onRetryAttempt = { attempts += it.context.attempt }) {
            if (it.attempt < 3) {
                throw IllegalStateException()
            }
        }

        assertEquals(listOf(1, 2), attempts)
    }

    @Test
    fun `onRetryAttempt is called before next attempt`() {
        val events = mutableListOf<String>()

        retryBlocking(onRetryAttempt = { events += "retry-${it.context.attempt}" }) {
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
