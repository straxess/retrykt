package io.github.straxess.retrykt

import io.github.straxess.retrykt.backoff.ConstantBackoff
import io.github.straxess.retrykt.backoff.NoBackoff
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

class RetryBlockingJsTest {

    @Test
    fun `retryBlocking with NoBackoff works`() {
        var attempts = 0

        val result = retryBlocking(backoff = NoBackoff) {
            attempts++

            if (attempts < 3) {
                error("fail")
            }

            "success"
        }

        assertEquals("success", result)
        assertEquals(3, attempts)
    }

    @Test
    fun `retryBlocking with Backoff fails`() {
        assertFailsWith<UnsupportedOperationException> {
            retryBlocking(backoff = ConstantBackoff(1.seconds)) {
                error("fail")
            }
        }
    }
}
