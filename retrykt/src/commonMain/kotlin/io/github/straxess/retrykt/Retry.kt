package io.github.straxess.retrykt

import io.github.straxess.retrykt.backoff.Backoff
import io.github.straxess.retrykt.backoff.NoBackoff
import io.github.straxess.retrykt.internal.sleep
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

public suspend fun <T> retry(
    maxAttempts: Int = Int.MAX_VALUE,
    backoff: Backoff = NoBackoff,
    shouldRetry: (Throwable) -> Boolean = { true },
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
            delay(delay)
            attempt++
        }
    }
}

public fun <T> retryBlocking(
    maxAttempts: Int = Int.MAX_VALUE,
    backoff: Backoff = NoBackoff,
    shouldRetry: (Throwable) -> Boolean = { true },
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
            sleep(delay)
            attempt++
        }
    }
}

public class RetryContext internal constructor(

    /**
     * Current attempt number. Starts from 1.
     */
    public val attempt: Int,

    /**
     * Maximum allowed attempts.
     */
    public val maxAttempts: Int,

    /**
     * Exception from the previous failed attempt.
     * Null on the first attempt.
     */
    public val lastThrowable: Throwable?
)
