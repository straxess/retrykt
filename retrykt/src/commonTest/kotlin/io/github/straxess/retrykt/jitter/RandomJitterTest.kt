package io.github.straxess.retrykt.jitter

import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RandomJitterTest {

    @Test
    fun `applies a random jitter`() {
        val maxJitter = 100.milliseconds
        val jitter = RandomJitter(maxJitter)
        val baseDelay = 10.seconds

        var actual: Duration?
        do {
            actual = jitter.apply(baseDelay)
        } while (actual == baseDelay)

        assertNotEquals(baseDelay, actual)
        assertTrue((actual - maxJitter) < baseDelay)
    }

    @Test
    fun `maxJitter can be zero`() {
        val jitter = RandomJitter(Duration.ZERO)
        val baseDelay = 10.seconds

        val actual = jitter.apply(baseDelay)

        assertEquals(baseDelay, actual)
    }

    @Test
    fun `RandomJitter throws IllegalArgumentException if maxJitter is negative`() {
        assertFailsWith<IllegalArgumentException> {
            RandomJitter((-10).milliseconds)
        }
    }
}
