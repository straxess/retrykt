package io.github.straxess.retrykt

import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.*

class RetryBlockingTest {

    @Test
    fun `simple retry`() {
        var attempts = 0

        retryBlocking {
            attempts += 1
        }

        assertEquals(1, attempts)
    }

    @Test
    fun `success after some failures`() {
        var attempts = 0

        retryBlocking {
            attempts += 1

            if (attempts < 5) {
                throw RuntimeException()
            }
        }

        assertEquals(5, attempts)
    }

    @Test
    fun `retry with max attempts`() {
        var attempts = 0

        assertFailsWith<RuntimeException> {
            retryBlocking(maxAttempts = 3) {
                attempts++
                throw RuntimeException()
            }
        }

        assertEquals(3, attempts)
    }

    @Test
    fun `does not retry when exception does not match shouldRetry`() {
        var attempts = 0

        assertFailsWith<IllegalStateException> {
            retryBlocking(shouldRetry = { it is IllegalArgumentException }) {
                attempts++
                throw IllegalStateException()
            }
        }

        assertEquals(1, attempts)
    }

    @Test
    fun `retries when exception matches shouldRetry`() {
        var attempts = 0

        retryBlocking(shouldRetry = { it is IllegalStateException }) {
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
            retryBlocking(shouldRetry = { error("should not be called") }) {
                throw CancellationException()
            }
        }
    }

    @Test
    fun `stops retrying when exception no longer matches shouldRetry`() {
        var attempts = 0

        assertFailsWith<IllegalArgumentException> {
            retryBlocking(shouldRetry = { it is IllegalStateException }) {
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

        assertFailsWith<RuntimeException> {
            retryBlocking(maxAttempts = maxAttempts) { context ->
                attempts += context.maxAttempts

                throw RuntimeException()
            }
        }

        assertEquals(listOf(3, 3, 3), attempts)
    }

    @Test
    fun `lastThrowable is null on first attempt`() {
        var throwable: Throwable? = RuntimeException()

        retryBlocking {
            throwable = it.lastThrowable
        }

        assertEquals(null, throwable)
    }

    @Test
    fun `lastThrowable contains previous exception`() {
        val exception = IllegalStateException("boom")

        var previous: Throwable? = null

        retryBlocking {
            previous = it.lastThrowable

            if (it.attempt == 1) {
                throw exception
            }
        }

        assertSame(exception, previous)
    }

    @Test
    fun `lastThrowable is updated after each failed attempt`() {
        val first = IllegalStateException()
        val second = IllegalArgumentException()

        val previous = mutableListOf<Throwable?>()

        retryBlocking(
            maxAttempts = 3,
            shouldRetry = { it is IllegalStateException || it is IllegalArgumentException }
        ) {
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

    @Test
    fun `retry throws IllegalArgumentException if maxAttempts is less than 1`() {
        assertFailsWith<IllegalArgumentException> {
            retryBlocking(maxAttempts = 0) {}
        }
    }

    @Test
    fun `onRetry receives current failure`() {
        val first = IllegalStateException()
        val second = IllegalArgumentException()

        val failures = mutableListOf<Throwable?>()

        retryBlocking(onRetry = { failures += it.context.lastThrowable }) {
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
    fun `onRetry receives current attempt`() {
        val attempts = mutableListOf<Int>()

        retryBlocking(onRetry = { attempts += it.context.attempt }) {
            if (it.attempt < 3) {
                throw IllegalStateException()
            }
        }

        assertEquals(listOf(1, 2), attempts)
    }

    @Test
    fun `onRetry is called before next attempt`() {
        val events = mutableListOf<String>()

        retryBlocking(onRetry = { events += "retry-${it.context.attempt}" }) {
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
