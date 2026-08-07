package io.github.straxess.retrykt

import kotlin.test.*

class RetryOnTest {

    @Test
    fun `default retries thrown outcome`() {
        val exception = IllegalStateException()
        val retryOn = RetryOn.default<String>()

        val actual = retryOn.shouldRetry(AttemptOutcome.Thrown(exception))

        assertTrue(actual)
    }

    @Test
    fun `default does not retry returned outcome`() {
        val retryOn = RetryOn.default<String>()

        val actual = retryOn.shouldRetry(AttemptOutcome.Returned("success"))

        assertFalse(actual)
    }

    @Test
    fun `thrown retries when predicate matches`() {
        val exception = IllegalStateException()
        val retryOn = RetryOn.thrown<String> { it is IllegalStateException }

        val actual = retryOn.shouldRetry(AttemptOutcome.Thrown(exception))

        assertTrue(actual)
    }

    @Test
    fun `thrown does not retry when predicate does not match`() {
        val exception = IllegalArgumentException()
        val retryOn = RetryOn.thrown<String> { it is IllegalStateException }

        val actual = retryOn.shouldRetry(AttemptOutcome.Thrown(exception))

        assertFalse(actual)
    }

    @Test
    fun `thrown does not retry returned outcome`() {
        val retryOn = RetryOn.thrown<String> { true }

        val actual = retryOn.shouldRetry(AttemptOutcome.Returned("success"))

        assertFalse(actual)
    }

    @Test
    fun `returned retries when predicate matches`() {
        val retryOn = RetryOn.returned<String> { it == "retry" }

        val actual = retryOn.shouldRetry(AttemptOutcome.Returned("retry"))

        assertTrue(actual)
    }

    @Test
    fun `returned does not retry when predicate does not match`() {
        val retryOn = RetryOn.returned<String> { it == "retry" }

        val actual = retryOn.shouldRetry(AttemptOutcome.Returned("success"))

        assertFalse(actual)
    }

    @Test
    fun `returned does not retry thrown outcome`() {
        val retryOn = RetryOn.returned<String> { true }

        val actual = retryOn.shouldRetry(AttemptOutcome.Thrown(IllegalStateException()))

        assertFalse(actual)
    }

    @Test
    fun `outcome can inspect returned outcome`() {
        val retryOn = RetryOn.outcome<String> { outcome ->
            when (outcome) {
                is AttemptOutcome.Returned -> outcome.value == "retry"
                is AttemptOutcome.Thrown -> false
            }
        }

        assertTrue(retryOn.shouldRetry(AttemptOutcome.Returned("retry")))
        assertFalse(retryOn.shouldRetry(AttemptOutcome.Returned("success")))
    }

    @Test
    fun `outcome can inspect thrown outcome`() {
        val exception = IllegalStateException()
        val retryOn = RetryOn.outcome<String> { outcome ->
            when (outcome) {
                is AttemptOutcome.Returned -> false
                is AttemptOutcome.Thrown -> outcome.throwable === exception
            }
        }

        val actual = retryOn.shouldRetry(AttemptOutcome.Thrown(exception))

        assertTrue(actual)
    }

    @Test
    fun `outcome receives original throwable instance`() {
        val exception = IllegalStateException()
        var received: Throwable? = null
        val retryOn = RetryOn.outcome<String> { outcome ->
            if (outcome is AttemptOutcome.Thrown) {
                received = outcome.throwable
            }

            true
        }

        retryOn.shouldRetry(AttemptOutcome.Thrown(exception))

        assertSame(exception, received)
    }

    @Test
    fun `RetryOn is contravariant`() {
        val anyRetryOn: RetryOn<Any> = RetryOn.thrown { true }

        val stringRetryOn: RetryOn<String> = anyRetryOn

        assertNotNull(stringRetryOn)
    }
}
