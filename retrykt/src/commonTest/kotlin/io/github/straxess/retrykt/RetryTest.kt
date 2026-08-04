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
    fun `simple retry`() = runTest {
        var attempts = 0

        retry {
            attempts += 1
        }

        assertEquals(1, attempts)
    }

    @Test
    fun `success after some failures`() = runTest {
        var attempts = 0

        retry {
            attempts += 1

            if (attempts < 5) {
                throw RuntimeException()
            }
        }

        assertEquals(5, attempts)
    }

    @Test
    fun `retry with max attempts`() = runTest {
        var attempts = 0

        assertFailsWith<RuntimeException> {
            retry(maxAttempts = 3) {
                attempts++
                throw RuntimeException()
            }
        }

        assertEquals(3, attempts)
    }

    @Test
    fun `does not retry when exception does not match shouldRetry`() = runTest {
        var attempts = 0

        assertFailsWith<IllegalStateException> {
            retry(shouldRetry = { it is IllegalArgumentException }) {
                attempts++
                throw IllegalStateException()
            }
        }

        assertEquals(1, attempts)
    }

    @Test
    fun `retries when exception matches shouldRetry`() = runTest {
        var attempts = 0

        retry(shouldRetry = { it is IllegalStateException }) {
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
            retry(shouldRetry = { error("should not be called") }) {
                throw CancellationException()
            }
        }
    }

    @Test
    fun `stops retrying when exception no longer matches shouldRetry`() = runTest {
        var attempts = 0

        assertFailsWith<IllegalArgumentException> {
            retry(shouldRetry = { it is IllegalStateException }) {
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

        assertFailsWith<RuntimeException> {
            retry(maxAttempts = maxAttempts) { context ->
                attempts += context.maxAttempts

                throw RuntimeException()
            }
        }

        assertEquals(listOf(3, 3, 3), attempts)
    }

    @Test
    fun `lastThrowable is null on first attempt`() = runTest {
        var throwable: Throwable? = RuntimeException()

        retry {
            throwable = it.lastThrowable
        }

        assertEquals(null, throwable)
    }

    @Test
    fun `lastThrowable contains previous exception`() = runTest {
        val exception = IllegalStateException("boom")

        var previous: Throwable? = null

        retry {
            previous = it.lastThrowable

            if (it.attempt == 1) {
                throw exception
            }
        }

        assertSame(exception, previous)
    }

    @Test
    fun `lastThrowable is updated after each failed attempt`() = runTest {
        val first = IllegalStateException()
        val second = IllegalArgumentException()

        val previous = mutableListOf<Throwable?>()

        retry(maxAttempts = 3, shouldRetry = { it is IllegalStateException || it is IllegalArgumentException }) {
            previous += it.lastThrowable

            when (it.attempt) {
                1 -> throw first
                2 -> throw second
                else -> {}
            }
        }

        assertEquals(3, previous.size)
        assertNull(previous[0])
        assertSame(first, previous[1])
        assertSame(second, previous[2])
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
    fun `onRetry receives current failure`() = runTest {
        val first = IllegalStateException()
        val second = IllegalArgumentException()

        val failures = mutableListOf<Throwable?>()

        retry(onRetry = { failures += it.context.lastThrowable }) {
            when (it.attempt) {
                1 -> throw first
                2 -> throw second
                else -> {}
            }
        }

        assertEquals(2, failures.size)
        assertSame(first, failures[0])
        assertSame(second, failures[1])
    }

    @Test
    fun `onRetry receives current attempt`() = runTest {
        val attempts = mutableListOf<Int>()

        retry(onRetry = { attempts += it.context.attempt }) {
            when (it.attempt) {
                1 -> throw IllegalStateException()
                2 -> throw IllegalStateException()
                else -> {}
            }
        }

        assertEquals(listOf(1, 2), attempts)
    }

    @Test
    fun `onRetry receives nextDelay`() = runTest {
        val nextDelays = mutableListOf<Duration>()

        retry(
            onRetry = { nextDelays += it.plan.nextDelay },
            backoff = object : Backoff {
                override fun nextDelay(attempt: Int) = 100.milliseconds * attempt
            }
        ) {
            when (it.attempt) {
                1 -> throw IllegalStateException()
                2 -> throw IllegalStateException()
                else -> {}
            }
        }

        assertEquals(listOf(100.milliseconds, 200.milliseconds), nextDelays)
    }

    @Test
    fun `onRetry is called before next attempt`() = runTest {
        val events = mutableListOf<String>()

        retry(onRetry = { events += "retry-${it.context.attempt}" }) {
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
