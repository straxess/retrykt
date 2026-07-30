package io.github.straxess.retrykt

import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.*

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
    fun `does not retry when exception does not match retryIf`() = runTest {
        var attempts = 0

        assertFailsWith<IllegalStateException> {
            retry(retryIf = { it is IllegalArgumentException }) {
                attempts++
                throw IllegalStateException()
            }
        }

        assertEquals(1, attempts)
    }

    @Test
    fun `retries when exception matches retryIf`() = runTest {
        var attempts = 0

        retry(retryIf = { it is IllegalStateException }) {
            attempts++
            if (attempts == 1) {
                throw IllegalStateException()
            }
        }

        assertEquals(2, attempts)
    }

    @Test
    fun `always rethrows CancellationException`() = runTest {
        var predicateCalled = false

        assertFailsWith<CancellationException> {
            retry(
                retryIf = {
                    predicateCalled = true
                    it is CancellationException
                }
            ) {
                throw CancellationException()
            }
        }

        assertFalse(predicateCalled)
    }

    @Test
    fun `stops retrying when exception no longer matches retryIf`() = runTest {
        var attempts = 0

        assertFailsWith<IllegalArgumentException> {
            retry(retryIf = { it is IllegalStateException }) {
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

            if (it.attempt < 3) {
                throw RuntimeException()
            }
        }

        assertEquals(listOf(0, 1, 2, 3), attempts)
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

            if (it.attempt == 0) {
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

        retry(maxAttempts = 3, retryIf = { it is IllegalStateException || it is IllegalArgumentException }) {
            previous += it.lastThrowable

            when (it.attempt) {
                0 -> throw first
                1 -> throw second
                else -> {}
            }
        }

        assertEquals(3, previous.size)
        assertNull(previous[0])
        assertSame(first, previous[1])
        assertSame(second, previous[2])
    }
}
