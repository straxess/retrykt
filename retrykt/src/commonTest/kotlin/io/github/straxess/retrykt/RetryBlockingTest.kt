package io.github.straxess.retrykt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
                throw RuntimeException("not enough")
            }
        }

        assertEquals(5, int)
    }

    @Test
    fun `retry with max attempts`() {
        assertFailsWith<Exception> {
            retryBlocking(maxAttempts = 3) {
                throw RuntimeException("not enough")
            }
        }
    }
}
