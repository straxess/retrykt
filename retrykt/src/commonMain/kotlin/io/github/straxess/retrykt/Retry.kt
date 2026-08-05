package io.github.straxess.retrykt

import io.github.straxess.retrykt.backoff.Backoff
import io.github.straxess.retrykt.backoff.NoBackoff
import io.github.straxess.retrykt.internal.sleep
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

/**
 * Retries the given [task] until it succeeds or the retry policy is exhausted.
 *
 * [CancellationException] is never retried and is rethrown immediately to preserve coroutine cancellation semantics.
 */
public suspend fun <T> retry(
    maxAttempts: Int = Int.MAX_VALUE,
    backoff: Backoff = NoBackoff,
    retryOn: RetryOn<T> = RetryOn.standard(),
    onRetryAttempt: suspend (RetryEvent<T>) -> Unit = {},
    task: suspend (RetryContext) -> T,
): T {
    require(maxAttempts > 0) {
        "maxAttempts must be greater than zero."
    }

    var attempt = 1
    while (true) {
        val retryContext = RetryContext(attempt, maxAttempts)

        val outcome = try {
            val returned = task(retryContext)
            AttemptOutcome.Returned(returned)
        } catch (t: Throwable) {
            if (t is CancellationException) {
                throw t
            }

            AttemptOutcome.Thrown(t)
        }

        if (!retryOn.shouldRetry(outcome)) {
            return when (outcome) {
                is AttemptOutcome.Returned -> outcome.value
                is AttemptOutcome.Thrown -> throw outcome.throwable
            }
        }

        if (attempt == maxAttempts) {
            throw RetryStoppedException(RetryStoppedReason.MaxAttemptsReached(maxAttempts))
        }

        val nextDelay = backoff.nextDelay(attempt)
        val retryEvent = RetryEvent(outcome, retryContext, RetryPlan(nextDelay))
        onRetryAttempt(retryEvent)

        delay(nextDelay)
        attempt++
    }
}


/**
 * Retries the given [task] until it succeeds or the retry policy is exhausted.
 *
 * [CancellationException] is never retried and is rethrown immediately.
 */
public fun <T> retryBlocking(
    maxAttempts: Int = Int.MAX_VALUE,
    backoff: Backoff = NoBackoff,
    retryOn: RetryOn<T> = RetryOn.standard(),
    onRetryAttempt: (RetryEvent<T>) -> Unit = {},
    task: (RetryContext) -> T
): T {
    require(maxAttempts > 0) {
        "maxAttempts must be greater than zero."
    }

    var attempt = 1
    while (true) {
        val retryContext = RetryContext(attempt, maxAttempts)

        val outcome = try {
            val returned = task(retryContext)
            AttemptOutcome.Returned(returned)
        } catch (t: Throwable) {
            if (t is CancellationException) {
                throw t
            }

            AttemptOutcome.Thrown(t)
        }

        if (!retryOn.shouldRetry(outcome)) {
            return when (outcome) {
                is AttemptOutcome.Returned -> outcome.value
                is AttemptOutcome.Thrown -> throw outcome.throwable
            }
        }

        if (attempt == maxAttempts) {
            throw RetryStoppedException(RetryStoppedReason.MaxAttemptsReached(maxAttempts))
        }

        val nextDelay = backoff.nextDelay(attempt)
        val retryEvent = RetryEvent(outcome, retryContext, RetryPlan(nextDelay))
        onRetryAttempt(retryEvent)

        sleep(nextDelay)
        attempt++
    }
}
