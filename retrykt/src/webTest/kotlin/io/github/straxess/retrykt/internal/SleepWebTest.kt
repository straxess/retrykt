package io.github.straxess.retrykt.internal

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

class SleepWebTest {

    @Test
    fun `sleep with positive duration fails`() {
        assertFailsWith<UnsupportedOperationException> {
            sleep(1.seconds)
        }
    }
}
