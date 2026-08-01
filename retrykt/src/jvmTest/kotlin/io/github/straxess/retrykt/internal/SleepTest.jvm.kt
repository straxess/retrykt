package io.github.straxess.retrykt.internal

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.TimeSource

class SleepTest {

    @Test
    fun `sleep waits at least requested duration`() {
        val mark = TimeSource.Monotonic.markNow()

        sleep(20.milliseconds)

        assertTrue(mark.elapsedNow() >= 20.milliseconds)
    }

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