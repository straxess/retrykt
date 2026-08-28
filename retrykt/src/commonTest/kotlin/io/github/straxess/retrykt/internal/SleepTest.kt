package io.github.straxess.retrykt.internal

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

class SleepTest {

    @Test
    fun `works with zero duration`() {
        sleep(Duration.ZERO)
    }

    @Test
    fun `throws IllegalArgumentException for negative duration`() {
        assertFailsWith<IllegalArgumentException> {
            sleep((-1).nanoseconds)
        }
    }

    @Test
    fun `throws IllegalArgumentException for infinite duration`() {
        assertFailsWith<IllegalArgumentException> {
            sleep(Duration.INFINITE)
        }
    }
}
