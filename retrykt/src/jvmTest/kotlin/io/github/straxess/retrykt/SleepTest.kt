package io.github.straxess.retrykt

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class SleepTest {

    @Test
    fun `sleep nanoseconds`() {
        val start = System.nanoTime()

        sleep(1.nanoseconds)

        val end = System.nanoTime()

        assertTrue(end - start >= 1)
    }

    @Test
    fun `sleep milliseconds`() {
        val start = System.currentTimeMillis()

        sleep(1.milliseconds)

        val end = System.currentTimeMillis()

        assertTrue(end - start >= 1)
    }

    @Test
    fun `sleep seconds`() {
        val start = System.currentTimeMillis()

        sleep(1.seconds)

        val end = System.currentTimeMillis()

        assertTrue(end - start >= 1_000)
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