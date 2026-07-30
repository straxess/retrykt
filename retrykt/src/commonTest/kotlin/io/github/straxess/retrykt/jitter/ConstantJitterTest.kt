package io.github.straxess.retrykt.jitter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ConstantJitterTest {

    @Test
    fun `applies a constant jitter`() {
        val constantJitter = 100.milliseconds
        val jitter = ConstantJitter(constantJitter)
        val baseDelay = 10.seconds

        val actual = jitter.apply(baseDelay)

        assertEquals(baseDelay + constantJitter, actual)
    }

    @Test
    fun `ConstantJitter throws IllegalArgumentException if jitter is negative`() {
        assertFailsWith<IllegalArgumentException> {
            ConstantJitter((-10).milliseconds)
        }
    }
}
