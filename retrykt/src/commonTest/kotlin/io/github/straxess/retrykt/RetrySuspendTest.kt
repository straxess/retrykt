package io.github.straxess.retrykt

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RetrySuspendTest {

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
}
