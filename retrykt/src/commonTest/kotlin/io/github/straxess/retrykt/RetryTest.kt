package io.github.straxess.retrykt

import io.github.straxess.retrykt.backoff.Backoff
import io.github.straxess.retrykt.backoff.BackoffContext
import io.github.straxess.retrykt.backoff.ConstantBackoff
import io.github.straxess.retrykt.listener.AttemptEvent
import io.github.straxess.retrykt.listener.RetryListener
import io.github.straxess.retrykt.listener.RetryPlan
import kotlinx.coroutines.*
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
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
    fun `stops with RetryExhaustedException when max attempts reached with thrown outcome`() = runTest {
        val maxAttempts = 3
        val expectedThrowable = RuntimeException()
        var attempts = 0

        val exception = assertFailsWith<RetryExhaustedException> {
            retry(maxAttempts = maxAttempts) {
                attempts++
                throw expectedThrowable
            }
        }

        assertEquals(3, attempts)
        assertTrue(exception.reason is RetryExhaustionReason.MaxAttemptsReached)
        assertEquals(maxAttempts, exception.reason.maxAttempts)
        assertTrue(exception.lastOutcome is AttemptOutcome.Thrown)
        assertSame(expectedThrowable, exception.lastOutcome.throwable)
        assertSame(expectedThrowable, exception.cause)
    }

    @Test
    fun `stops with RetryExhaustedException when max attempts reached with returned outcome`() = runTest {
        val maxAttempts = 3
        val expectedReturned = 1
        var attempts = 0

        val exception = assertFailsWith<RetryExhaustedException> {
            retry(maxAttempts = maxAttempts, retryOn = RetryOn.returned { it == 1 }) {
                attempts++
                expectedReturned
            }
        }

        assertEquals(3, attempts)
        assertTrue(exception.reason is RetryExhaustionReason.MaxAttemptsReached)
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
    fun `cancellation after task returns prevents retry collaborators and listeners`() = runTest {
        val job = Job()
        var retryOnCalled = false
        var backoffCalled = false
        var jitterCalled = false
        var listenerCalled = false

        assertFailsWith<CancellationException> {
            withContext(job) {
                retry(
                    retryOn = RetryOn.outcome<String> {
                        retryOnCalled = true
                        true
                    },
                    backoff = object : Backoff {
                        override fun calculateDelay(context: BackoffContext): Duration {
                            backoffCalled = true
                            return Duration.ZERO
                        }
                    },
                    jitter = {
                        jitterCalled = true
                        it
                    },
                    listener = RetryListener(
                        onRetry = { _, _ -> listenerCalled = true },
                        onSuccess = { listenerCalled = true },
                        onFailure = { listenerCalled = true },
                    ),
                ) {
                    job.cancel()
                    "result"
                }
            }
        }

        assertFalse(retryOnCalled)
        assertFalse(backoffCalled)
        assertFalse(jitterCalled)
        assertFalse(listenerCalled)
    }

    @Test
    fun `cancellation during retry predicate prevents accepted outcome and terminal callbacks`() = runTest {
        val job = Job()
        val listenerEvents = mutableListOf<String>()

        assertFailsWith<CancellationException> {
            withContext(job) {
                retry(
                    retryOn = RetryOn.outcome<String> {
                        job.cancel()
                        false
                    },
                    listener = RetryListener(
                        onRetry = { _, _ -> listenerEvents += "retry" },
                        onSuccess = { listenerEvents += "success" },
                        onFailure = { listenerEvents += "failure" },
                    ),
                ) {
                    "result"
                }
            }
        }

        assertTrue(listenerEvents.isEmpty())
    }

    @Test
    fun `cancellation during retry predicate prevents exhausted outcome and terminal callbacks`() = runTest {
        val job = Job()
        val listenerEvents = mutableListOf<String>()

        assertFailsWith<CancellationException> {
            withContext(job) {
                retry(
                    maxAttempts = 1,
                    retryOn = RetryOn.outcome<String> {
                        job.cancel()
                        true
                    },
                    listener = RetryListener(
                        onRetry = { _, _ -> listenerEvents += "retry" },
                        onSuccess = { listenerEvents += "success" },
                        onFailure = { listenerEvents += "failure" },
                    ),
                ) {
                    "result"
                }
            }
        }

        assertTrue(listenerEvents.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `cancellation during delay prevents the next attempt and terminal callbacks`() = runTest {
        var attempts = 0
        val listenerEvents = mutableListOf<String>()

        val job =
            launch {
                retry(
                    backoff = ConstantBackoff(1.days),
                    listener = RetryListener(
                        onRetry = { _, _ -> listenerEvents += "retry" },
                        onSuccess = { listenerEvents += "success" },
                        onFailure = { listenerEvents += "failure" },
                    ),
                ) {
                    attempts++
                    error("retry")
                }
            }

        runCurrent()
        assertEquals(1, attempts)
        assertEquals(listOf("retry"), listenerEvents)

        job.cancelAndJoin()

        assertTrue(job.isCancelled)
        assertEquals(1, attempts)
        assertEquals(listOf("retry"), listenerEvents)
    }

    @Test
    fun `throws IllegalStateException if backoff returns negative delay`() = runTest {
        assertFailsWith<IllegalStateException> {
            retry(
                backoff = object : Backoff {
                    override fun calculateDelay(context: BackoffContext) = (-1).milliseconds
                },
            ) {
                error("task should not succeed")
            }
        }
    }

    @Test
    fun `throws IllegalStateException if jitter returns negative delay`() = runTest {
        assertFailsWith<IllegalStateException> {
            retry(jitter = { (-1).milliseconds }) {
                error("task should not succeed")
            }
        }
    }

    @Test
    fun `propagates exceptions from retry collaborators unchanged`() = runTest {
        val retryOnException = RuntimeException("retryOn")
        val backoffException = RuntimeException("backoff")
        val jitterException = RuntimeException("jitter")

        val actualRetryOnException =
            assertFailsWith<RuntimeException> {
                retry(retryOn = RetryOn.outcome<Unit> { throw retryOnException }) {}
            }
        val actualBackoffException =
            assertFailsWith<RuntimeException> {
                retry(
                    backoff = object : Backoff {
                        override fun calculateDelay(context: BackoffContext): Duration = throw backoffException
                    },
                ) {
                    error("retry")
                }
            }
        val actualJitterException =
            assertFailsWith<RuntimeException> {
                retry(jitter = { throw jitterException }) { error("retry") }
            }

        assertSame(retryOnException, actualRetryOnException)
        assertSame(backoffException, actualBackoffException)
        assertSame(jitterException, actualJitterException)
    }

    @Test
    fun `propagates exceptions from listener callbacks unchanged`() = runTest {
        val retryException = RuntimeException("onRetry")
        val successException = RuntimeException("onSuccess")
        val failureException = RuntimeException("onFailure")

        val actualRetryException =
            assertFailsWith<RuntimeException> {
                retry(listener = RetryListener(onRetry = { _, _ -> throw retryException })) { error("retry") }
            }
        val actualSuccessException =
            assertFailsWith<RuntimeException> {
                retry(listener = RetryListener(onSuccess = { throw successException })) {}
            }
        val actualFailureException =
            assertFailsWith<RuntimeException> {
                retry(
                    maxAttempts = 1,
                    listener = RetryListener(onFailure = { throw failureException }),
                ) {
                    error("retry")
                }
            }

        assertSame(retryException, actualRetryException)
        assertSame(successException, actualSuccessException)
        assertSame(failureException, actualFailureException)
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

        assertFailsWith<RetryExhaustedException> {
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
                override fun calculateDelay(context: BackoffContext) = 20.milliseconds
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

        val events = mutableListOf<AttemptEvent<*>>()

        retry(
            listener = RetryListener(onRetry = { event, _ -> events += event }),
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
        val events = mutableListOf<AttemptEvent<*>>()

        retry(
            retryOn = RetryOn.returned { it == "retry" },
            listener = RetryListener(onRetry = { event, _ -> events += event }),
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
    fun `onRetry receives event and plan`() = runTest {
        val callbacks = mutableListOf<Pair<AttemptEvent<*>, RetryPlan>>()

        retry(
            backoff = ConstantBackoff(100.milliseconds),
            retryOn = RetryOn.returned { it == "retry" },
            listener = RetryListener(
                onRetry = { event, plan -> callbacks += event to plan },
            ),
        ) {
            if (it.attempt < 2) {
                "retry"
            } else {
                "success"
            }
        }

        assertEquals(1, callbacks.size)

        val (event, plan) = callbacks.single()

        assertTrue(event.outcome is AttemptOutcome.Returned)
        assertEquals("retry", event.outcome.value)
        assertEquals(100.milliseconds, plan.nextAppliedDelay)
    }

    @Test
    fun `onRetry is called before next attempt`() = runTest {
        val events = mutableListOf<String>()

        retry(
            listener = RetryListener(onRetry = { event, _ -> events += "retry-${event.context.attempt}" }),
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
        val events = mutableListOf<AttemptEvent<*>>()

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
        val events = mutableListOf<AttemptEvent<*>>()

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
        val events = mutableListOf<AttemptEvent<*>>()

        assertFailsWith<IllegalStateException> {
            retry(
                retryOn = RetryOn.thrown { false },
                listener = RetryListener(onFailure = { events += it }),
            ) {
                throw IllegalStateException()
            }
        }

        assertEquals(1, events.size)
        assertTrue(events.single().outcome is AttemptOutcome.Thrown)
    }

    @Test
    fun `onFailure receives last outcome when max attempts are reached`() = runTest {
        val exception = IllegalStateException()
        val events = mutableListOf<AttemptEvent<*>>()

        assertFailsWith<RetryExhaustedException> {
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
                onRetry = { event, _ -> events += "retry-${event.context.attempt}" },
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
    fun `listener receives retry and terminal failure events`() = runTest {
        val events = mutableListOf<String>()

        assertFailsWith<RetryExhaustedException> {
            retry(
                maxAttempts = 3,
                listener = RetryListener(
                    onRetry = { event, _ -> events += "retry-${event.context.attempt}" },
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
        val successEvents = mutableListOf<AttemptEvent<*>>()

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
        val attemptEvents = mutableListOf<AttemptEvent<*>>()

        assertFailsWith<RetryExhaustedException> {
            retry(
                maxAttempts = 2,
                listener = RetryListener(onRetry = { event, _ -> attemptEvents += event }),
            ) {
                throw IllegalStateException()
            }
        }

        assertEquals(1, attemptEvents.size)
        assertEquals(1, attemptEvents.single().context.attempt)
    }

    @Test
    fun `onFailure receives returned outcome when max attempts are reached`() = runTest {
        val events = mutableListOf<AttemptEvent<*>>()

        assertFailsWith<RetryExhaustedException> {
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
                    onRetry = { _, _ -> events += "retry" },
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
    fun `jitter receives backoff delay from backoff`() = runTest {
        val backoffDelays = mutableListOf<Duration>()

        retry(
            maxAttempts = 2,
            backoff = object : Backoff {
                override fun calculateDelay(context: BackoffContext) = 100.milliseconds
            },
            jitter = {
                backoffDelays += it
                it
            },
        ) {
            if (it.attempt == 1) {
                throw IllegalStateException()
            }
        }

        assertEquals(listOf(100.milliseconds), backoffDelays)
    }

    @Test
    fun `backoff receives prev applied delay`() = runTest {
        val prevAppliedDelays = mutableListOf<Duration?>()

        retry(
            backoff = object : Backoff {
                override fun calculateDelay(context: BackoffContext): Duration {
                    prevAppliedDelays += context.prevAppliedDelay
                    return 100.milliseconds * context.attempt
                }
            },
            jitter = { it + 50.milliseconds },
        ) {
            if (it.attempt < 4) {
                throw IllegalStateException()
            }
        }

        assertEquals(listOf(null, 150.milliseconds, 250.milliseconds), prevAppliedDelays)
    }

    @Test
    fun `onRetry receives current outcome and previous outcome in context`() = runTest {
        val attemptEvents = mutableListOf<AttemptEvent<*>>()

        val result = retry(
            retryOn = RetryOn.returned { it == "first" || it == "second" },
            listener = RetryListener(onRetry = { event, _ -> attemptEvents += event }),
        ) {
            when (it.attempt) {
                1 -> "first"
                2 -> "second"
                else -> "third"
            }
        }

        assertEquals(2, attemptEvents.size)
        assertEquals("third", result)

        assertEquals("first", (attemptEvents[0].outcome as AttemptOutcome.Returned).value)
        assertEquals(null, attemptEvents[0].context.prevOutcome)

        assertEquals("second", (attemptEvents[1].outcome as AttemptOutcome.Returned).value)

        val prevOutcome = attemptEvents[1].context.prevOutcome
        assertTrue(prevOutcome is AttemptOutcome.Returned)
        assertEquals("first", prevOutcome.value)
    }

    @Test
    fun `throws IllegalStateException if backoff returns infinite delay`() = runTest {
        assertFailsWith<IllegalStateException> {
            retry(
                backoff = object : Backoff {
                    override fun calculateDelay(context: BackoffContext) = Duration.INFINITE
                },
            ) {
                error("task should not succeed")
            }
        }
    }

    @Test
    fun `throws IllegalStateException if jitter returns infinite delay`() = runTest {
        assertFailsWith<IllegalStateException> {
            retry(jitter = { Duration.INFINITE }) {
                error("task should not succeed")
            }
        }
    }

    @Test
    fun `invalid strategy delay does not notify listener`() = runTest {
        var listenerCalled = false

        assertFailsWith<IllegalStateException> {
            retry(
                backoff = object : Backoff {
                    override fun calculateDelay(context: BackoffContext): Duration = Duration.INFINITE
                },
                listener = RetryListener(
                    onRetry = { _, _ -> listenerCalled = true },
                    onSuccess = { listenerCalled = true },
                    onFailure = { listenerCalled = true },
                ),
            ) {
                error("retry")
            }
        }

        assertFalse(listenerCalled)
    }
}
