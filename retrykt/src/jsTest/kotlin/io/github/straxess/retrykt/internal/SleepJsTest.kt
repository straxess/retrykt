package io.github.straxess.retrykt.internal

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

class SleepJsTest {

    @Test
    fun `sleep with positive duration fails`() {
        assertFailsWith<UnsupportedOperationException> {
            sleep(1.seconds)
        }
    }
}
