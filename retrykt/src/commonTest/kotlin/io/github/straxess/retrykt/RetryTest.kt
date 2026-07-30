package io.github.straxess.retrykt

import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class RetryTest {

    @Test
    fun `simple retry`() = runTest {
        var int = 0

        retry {
            int += 1
        }

        assertEquals(1, int)
    }

    @Test
    fun `success after some failures`() = runTest {
        var int = 0

        retry {
            int += 1

            if (int < 5) {
                throw RuntimeException("not enough")
            }
        }

        assertEquals(5, int)
    }

    @Test
    fun `retry with max attempts`() = runTest {
        assertFailsWith<Exception> {
            retry(maxAttempts = 3) {
                throw RuntimeException("not enough")
            }
        }
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
}
