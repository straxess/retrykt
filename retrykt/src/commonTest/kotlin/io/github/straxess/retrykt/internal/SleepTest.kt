package io.github.straxess.retrykt.internal

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

class SleepTest {

    @Test
    fun `works with zero`() {
        sleep(Duration.ZERO)
    }

    @Test
    fun `throws IllegalArgumentException if duration is negative`() {
        assertFailsWith<IllegalArgumentException> {
            sleep((-1).nanoseconds)
        }
    }
}
