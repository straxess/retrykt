package io.github.straxess.retrykt.internal

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class SleepJvmTest {

    @Test
    fun `sleep waits at least requested duration`() {
        val mark = TimeSource.Monotonic.markNow()

        sleep(20.milliseconds)

        assertTrue(mark.elapsedNow() >= 20.milliseconds)
    }
}
