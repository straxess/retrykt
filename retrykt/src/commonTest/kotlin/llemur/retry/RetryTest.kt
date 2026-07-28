package llemur.retry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RetryTest {

    @Test
    fun `simple retry`() {
        var int = 0

        Retry().execute {
            int += 1
        }

        assertEquals(1, int)
    }

    @Test
    fun `success after some failures`() {
        var int = 0

        Retry().execute {
            int += 1

            if (int < 5) {
                throw RuntimeException("not enough")
            }
        }

        assertEquals(5, int)
    }

    @Test
    fun `retry with max invocations`() {
        assertFailsWith<Exception> {
            Retry(maxTries = 3).execute {
                throw RuntimeException("not enough")
            }
        }
    }
}
