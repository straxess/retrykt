package io.github.straxess.retrykt

import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class RetryBlockingTest {

    @Test
    fun `simple retry`() {
        var int = 0

        retryBlocking {
            int += 1
        }

        assertEquals(1, int)
    }

    @Test
    fun `success after some failures`() {
        var int = 0

        retryBlocking {
            int += 1

            if (int < 5) {
                throw RuntimeException()
            }
        }

        assertEquals(5, int)
    }

    @Test
    fun `retry with max attempts`() {
        assertFailsWith<Exception> {
            retryBlocking(maxAttempts = 3) {
                throw RuntimeException()
            }
        }
    }

    @Test
    fun `does not retry when exception does not match retryIf`() {
        var attempts = 0

        assertFailsWith<IllegalStateException> {
            retryBlocking(retryIf = { it is IllegalArgumentException }) {
                attempts++
                throw IllegalStateException()
            }
        }

        assertEquals(1, attempts)
    }

    @Test
    fun `retries when exception matches retryIf`() {
        var attempts = 0

        retryBlocking(retryIf = { it is IllegalStateException }) {
            attempts++
            if (attempts == 1) {
                throw IllegalStateException()
            }
        }

        assertEquals(2, attempts)
    }

    @Test
    fun `always rethrows CancellationException`() {
        var predicateCalled = false

        assertFailsWith<CancellationException> {
            retryBlocking(
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
