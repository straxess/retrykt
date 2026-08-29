package io.github.straxess.retrykt.listener

import io.github.straxess.retrykt.AttemptOutcome
import io.github.straxess.retrykt.RetryContext
import io.github.straxess.retrykt.RetryEvent
import kotlin.test.Test

class RetryListenerTest {

    @Test
    fun `default callbacks do nothing`() {
        val event = RetryEvent(
            outcome = AttemptOutcome.Returned(1),
            context = RetryContext(
                attempt = 2,
                maxAttempts = 10,
                prevOutcome = AttemptOutcome.Returned(0),
            ),
        )

        val listener = object : RetryListener {}

        listener.onRetry(event)
        listener.onSuccess(event)
        listener.onFailure(event)
    }

    @Test
    fun `null callbacks do nothing`() {
        val event = RetryEvent(
            outcome = AttemptOutcome.Returned(1),
            context = RetryContext(
                attempt = 2,
                maxAttempts = 10,
                prevOutcome = AttemptOutcome.Returned(0),
            ),
        )

        val listener = RetryListener(
            onRetry = null,
            onSuccess = null,
            onFailure = null,
        )

        listener.onRetry(event)
        listener.onSuccess(event)
        listener.onFailure(event)
    }
}
