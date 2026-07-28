package llemur.retry

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RetrySuspendTest {

    @Test
    fun `simple retry`() = runTest {
        var int = 0

        Retry().executeSuspend {
            int += 1
        }

        assertEquals(1, int)
    }

    @Test
    fun `success after some failures`() = runTest {
        var int = 0

        Retry().executeSuspend {
            int += 1

            if (int < 5) {
                throw RuntimeException("not enough")
            }
        }

        assertEquals(5, int)
    }

    @Test
    fun `retry with max invocations`() = runTest {
        assertFailsWith<Exception> {
            Retry(maxTries = 3).executeSuspend() {
                throw RuntimeException("not enough")
            }
        }
    }
}
