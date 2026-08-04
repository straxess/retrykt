package io.github.straxess.retrykt

import io.github.straxess.retrykt.backoff.Backoff
import io.github.straxess.retrykt.backoff.NoBackoff
import io.github.straxess.retrykt.internal.sleep
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

/**
 * Retries the given [task] until it succeeds or the retry policy is exhausted.
 *
 * Note: [CancellationException] is never retried and is always rethrown immediately,
 * including in [retryBlocking], to preserve coroutine cancellation semantics.
 */
public suspend fun <T> retry(
    maxAttempts: Int = Int.MAX_VALUE,
    backoff: Backoff = NoBackoff,
    shouldRetry: (Throwable) -> Boolean = { true },
    onRetry: suspend (RetryEvent) -> Unit = {},
    task: suspend (RetryContext) -> T
): T {
    require(maxAttempts > 0) {
        "maxAttempts must be greater than zero."
    }

    var attempt = 1
    var lastThrowable: Throwable? = null
    while (true) {
        try {
            val retryContext = RetryContext(attempt, maxAttempts, lastThrowable)
            return task(retryContext)
        } catch (t: Throwable) {
            if (t is CancellationException) {
                throw t
            }

            if (attempt >= maxAttempts) {
                throw t
            }

            if (!shouldRetry(t)) {
                throw t
            }

            lastThrowable = t
            val delay = backoff.nextDelay(attempt)

            val retryContext = RetryContext(attempt, maxAttempts, lastThrowable)
            val retryPlan = RetryPlan(delay)
            val retryEvent = RetryEvent(retryContext, retryPlan)
            onRetry(retryEvent)

            delay(delay)
            attempt++
        }
    }
}

/**
 * Retries the given [task] until it succeeds or the retry policy is exhausted.
 *
 * Note: [CancellationException] is never retried and is always rethrown immediately,
 * including in [retryBlocking], to preserve coroutine cancellation semantics.
 */
public fun <T> retryBlocking(
    maxAttempts: Int = Int.MAX_VALUE,
    backoff: Backoff = NoBackoff,
    shouldRetry: (Throwable) -> Boolean = { true },
    onRetry: (RetryEvent) -> Unit = {},
    task: (RetryContext) -> T
): T {
    require(maxAttempts > 0) {
        "maxAttempts must be greater than zero."
    }

    var attempt = 1
    var lastThrowable: Throwable? = null
    while (true) {
        try {
            val retryContext = RetryContext(attempt, maxAttempts, lastThrowable)
            return task(retryContext)
        } catch (t: Throwable) {
            if (t is CancellationException) {
                throw t
            }

            if (attempt >= maxAttempts) {
                throw t
            }

            if (!shouldRetry(t)) {
                throw t
            }

            lastThrowable = t
            val delay = backoff.nextDelay(attempt)

            val retryContext = RetryContext(attempt, maxAttempts, lastThrowable)
            val retryPlan = RetryPlan(delay)
            val retryEvent = RetryEvent(retryContext, retryPlan)
            onRetry(retryEvent)

            sleep(delay)
            attempt++
        }
    }
}
