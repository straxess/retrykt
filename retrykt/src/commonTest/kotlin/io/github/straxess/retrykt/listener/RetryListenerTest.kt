package io.github.straxess.retrykt.listener

import io.github.straxess.retrykt.AttemptOutcome
import io.github.straxess.retrykt.RetryContext
import kotlin.test.Test
import kotlin.time.Duration

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

        val decision = RetryDecision(Duration.ZERO)

        val listener = object : RetryListener {}

        listener.onRetry(event, decision)
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

        val decision = RetryDecision(Duration.ZERO)

        val listener = RetryListener(
            onRetry = null,
            onSuccess = null,
            onFailure = null,
        )

        listener.onRetry(event, decision)
        listener.onSuccess(event)
        listener.onFailure(event)
    }
}
