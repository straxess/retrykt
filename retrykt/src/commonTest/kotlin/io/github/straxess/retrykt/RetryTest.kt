package io.github.straxess.retrykt

import io.github.straxess.retrykt.backoff.Backoff
import io.github.straxess.retrykt.backoff.BackoffContext
import io.github.straxess.retrykt.listener.RetryListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
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
    fun `stops with RetryStoppedException when max attempts reached with thrown outcome`() = runTest {
        val maxAttempts = 3
        val expectedThrowable = RuntimeException()
        var attempts = 0

        val exception = assertFailsWith<RetryStoppedException> {
            retry(maxAttempts = maxAttempts) {
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
    fun `stops with RetryStoppedException when max attempts reached with returned outcome`() = runTest {
        val maxAttempts = 3
        val expectedReturned = 1
        var attempts = 0

        val exception = assertFailsWith<RetryStoppedException> {
            retry(maxAttempts = maxAttempts, retryOn = RetryOn.returned { it == 1 }) {
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
    fun `does not invoke task in an already cancelled coroutine`() = runTest {
        val job = Job().also { it.cancel() }
        var invoked = false

        assertFailsWith<CancellationException> {
            withContext(job) {
                retry {
                    invoked = true
                }
            }
        }

        assertFalse(invoked)
    }

    @Test
    fun `rejects invalid custom delays`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            retry(
                backoff = object : Backoff {
                    override fun nextDelay(context: BackoffContext) = (-1).milliseconds
                },
            ) {
                error("task should not succeed")
            }
        }

        assertFailsWith<IllegalArgumentException> {
            retry(jitter = { Duration.INFINITE }) {
                error("task should not succeed")
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
                override fun nextDelay(context: BackoffContext) = 20.milliseconds
            },
            jitter = { it + 10.milliseconds },
        ) {
            attempts++
            if (attempts == 1) {
                throw RuntimeException("fail")
            }
        }

        assertEquals(2, attempts)
        assertEquals(30, currentTime)
    }

    @Test
    fun `retry throws IllegalArgumentException if maxAttempts is less than 1`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            retry(maxAttempts = 0) {}
        }
    }

    @Test
    fun `onRetry receives thrown outcome`() = runTest {
        val first = IllegalStateException()
        val second = IllegalArgumentException()

        val events = mutableListOf<RetryEvent<*>>()

        retry(
            listener = RetryListener(onRetry = { events += it }),
        ) {
            when (it.attempt) {
                1 -> throw first
                2 -> throw second
                else -> Unit
            }
        }

        assertEquals(2, events.size)

        assertTrue(events[0].outcome is AttemptOutcome.Thrown)
        assertTrue(events[1].outcome is AttemptOutcome.Thrown)

        assertSame(
            first,
            (events[0].outcome as AttemptOutcome.Thrown).throwable,
        )
        assertSame(
            second,
            (events[1].outcome as AttemptOutcome.Thrown).throwable,
        )
    }

    @Test
    fun `onRetry receives returned outcome`() = runTest {
        val events = mutableListOf<RetryEvent<*>>()

        retry(
            retryOn = RetryOn.returned { it == "retry" },
            listener = RetryListener(onRetry = { events += it }),
        ) {
            if (it.attempt < 2) {
                "retry"
            } else {
                "success"
            }
        }

        assertEquals(1, events.size)

        val outcome = events.single().outcome

        assertTrue(outcome is AttemptOutcome.Returned)
        assertEquals("retry", outcome.value)
    }

    @Test
    fun `onRetry is called before next attempt`() = runTest {
        val events = mutableListOf<String>()

        retry(
            listener = RetryListener(onRetry = { events += "retry-${it.context.attempt}" }),
        ) {
            events += "attempt-${it.attempt}"

            if (it.attempt < 3) {
                throw IllegalStateException()
            }
        }

        assertEquals(
            listOf("attempt-1", "retry-1", "attempt-2", "retry-2", "attempt-3"),
            events,
        )
    }

    @Test
    fun `onSuccess receives successful outcome`() = runTest {
        val events = mutableListOf<RetryEvent<*>>()

        retry(
            listener = RetryListener(onSuccess = { events += it }),
        ) {
            "success"
        }

        assertEquals(1, events.size)

        val event = events.single()

        assertTrue(event.outcome is AttemptOutcome.Returned)
        assertEquals("success", event.outcome.value)
        assertEquals(1, event.context.attempt)
    }

    @Test
    fun `onSuccess receives final successful outcome after retries`() = runTest {
        val events = mutableListOf<RetryEvent<*>>()

        retry(
            listener = RetryListener(onSuccess = { events += it }),
        ) {
            if (it.attempt < 3) {
                throw IllegalStateException()
            }

            "success"
        }

        assertEquals(1, events.size)

        val event = events.single()

        assertEquals(3, event.context.attempt)
        assertTrue(event.outcome is AttemptOutcome.Returned)
        assertEquals(
            "success",
            event.outcome.value,
        )
    }

    @Test
    fun `onFailure receives non-retryable thrown outcome`() = runTest {
        val throwable = IllegalStateException()
        val events = mutableListOf<RetryEvent<*>>()

        assertFailsWith<IllegalStateException> {
            retry(
                retryOn = RetryOn.thrown { false },
                listener = RetryListener(onFailure = { events += it }),
            ) {
                throw throwable
            }
        }

        assertEquals(1, events.size)

        val event = events.single()

        assertEquals(1, event.context.attempt)
        assertTrue(event.outcome is AttemptOutcome.Thrown)
        assertSame(throwable, event.outcome.throwable)
    }

    @Test
    fun `onFailure receives last outcome when max attempts are reached`() = runTest {
        val exception = IllegalStateException()
        val events = mutableListOf<RetryEvent<*>>()

        assertFailsWith<RetryStoppedException> {
            retry(
                maxAttempts = 2,
                listener = RetryListener(onFailure = { events += it }),
            ) {
                throw exception
            }
        }

        assertEquals(1, events.size)

        val event = events.single()

        assertEquals(2, event.context.attempt)
        assertTrue(event.outcome is AttemptOutcome.Thrown)
        assertSame(exception, event.outcome.throwable)
    }

    @Test
    fun `listener receives correct lifecycle`() = runTest {
        val events = mutableListOf<String>()

        retry(
            listener = RetryListener(
                onRetry = { events += "retry-${it.context.attempt}" },
                onSuccess = { events += "success-${it.context.attempt}" },
                onFailure = { events += "failure-${it.context.attempt}" },
            ),
        ) {
            if (it.attempt < 3) {
                throw IllegalStateException()
            }
        }

        assertEquals(
            listOf("retry-1", "retry-2", "success-3"),
            events,
        )
    }

    @Test
    fun `listener receives retry and failure on exhaustion`() = runTest {
        val events = mutableListOf<String>()

        assertFailsWith<RetryStoppedException> {
            retry(
                maxAttempts = 3,
                listener = RetryListener(
                    onRetry = { events += "retry-${it.context.attempt}" },
                    onSuccess = { events += "success-${it.context.attempt}" },
                    onFailure = { events += "failure-${it.context.attempt}" },
                ),
            ) {
                throw IllegalStateException()
            }
        }

        assertEquals(
            listOf("retry-1", "retry-2", "failure-3"),
            events,
        )
    }

    @Test
    fun `onSuccess is not called for retryable returned outcome`() = runTest {
        val successEvents = mutableListOf<RetryEvent<*>>()

        retry(
            retryOn = RetryOn.returned { it == "retry" },
            listener = RetryListener(onSuccess = { successEvents += it }),
        ) {
            if (it.attempt < 2) {
                "retry"
            } else {
                "success"
            }
        }

        assertEquals(1, successEvents.size)
        assertEquals(2, successEvents.single().context.attempt)
    }

    @Test
    fun `onRetry is not called when max attempts are reached`() = runTest {
        val retryEvents = mutableListOf<RetryEvent<*>>()

        assertFailsWith<RetryStoppedException> {
            retry(
                maxAttempts = 2,
                listener = RetryListener(onRetry = { retryEvents += it }),
            ) {
                throw IllegalStateException()
            }
        }

        assertEquals(1, retryEvents.size)
        assertEquals(1, retryEvents.single().context.attempt)
    }

    @Test
    fun `onFailure receives last returned outcome when max attempts are reached`() = runTest {
        val events = mutableListOf<RetryEvent<*>>()

        assertFailsWith<RetryStoppedException> {
            retry(
                maxAttempts = 2,
                retryOn = RetryOn.returned { it == "retry" },
                listener = RetryListener(onFailure = { events += it }),
            ) {
                "retry"
            }
        }

        assertEquals(1, events.size)

        val event = events.single()

        assertEquals(2, event.context.attempt)
        assertTrue(event.outcome is AttemptOutcome.Returned)
        assertEquals("retry", event.outcome.value)
    }

    @Test
    fun `does not notify listener for cancellation`() = runTest {
        val events = mutableListOf<String>()

        assertFailsWith<CancellationException> {
            retry(
                listener = RetryListener(
                    onRetry = { events += "retry" },
                    onSuccess = { events += "success" },
                    onFailure = { events += "failure" },
                ),
            ) {
                throw CancellationException()
            }
        }

        assertTrue(events.isEmpty())
    }

    @Test
    fun `jitter receives raw delay from backoff`() = runTest {
        val rawDelays = mutableListOf<Duration>()

        retry(
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
    fun `backoff receives last applied delay`() = runTest {
        val lastAppliedDelays = mutableListOf<Duration?>()

        retry(
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
