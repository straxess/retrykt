package io.github.straxess.retrykt

import io.github.straxess.retrykt.backoff.Backoff
import io.github.straxess.retrykt.backoff.NoBackoff
import io.github.straxess.retrykt.internal.sleep
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

public suspend fun <T> retry(
    maxAttempts: Int = Int.MAX_VALUE,
    backoff: Backoff = NoBackoff,
    retryIf: (Throwable) -> Boolean = { true },
    task: suspend (RetryContext) -> T
): T {
    require(maxAttempts > 0) {
        "maxAttempts must be greater than zero."
    }

    var attempt = 0
    var lastThrowable: Throwable? = null
    while (true) {
        try {
            val result = task(RetryContext(attempt = attempt, lastThrowable = lastThrowable))
            return result
        } catch (t: Throwable) {
            if (t is CancellationException) {
                throw t
            }

            if (attempt + 1 >= maxAttempts) {
                throw t
            }

            if (!retryIf(t)) {
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
    retryIf: (Throwable) -> Boolean = { true },
    task: (RetryContext) -> T
): T {
    require(maxAttempts > 0) {
        "maxAttempts must be greater than zero."
    }

    var attempt = 0
    var lastThrowable: Throwable? = null
    while (true) {
        try {
            val result = task(RetryContext(attempt = attempt, lastThrowable = lastThrowable))
            return result
        } catch (t: Throwable) {
            if (t is CancellationException) {
                throw t
            }

            if (attempt + 1 >= maxAttempts) {
                throw t
            }

            if (!retryIf(t)) {
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
    public val attempt: Int,
    public val lastThrowable: Throwable?
)
