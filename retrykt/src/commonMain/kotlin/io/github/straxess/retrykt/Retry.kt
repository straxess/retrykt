package io.github.straxess.retrykt

import io.github.straxess.retrykt.backoff.Backoff
import io.github.straxess.retrykt.backoff.BackoffContext
import io.github.straxess.retrykt.backoff.NoBackoff
import io.github.straxess.retrykt.internal.sleep
import io.github.straxess.retrykt.jitter.Jitter
import io.github.straxess.retrykt.jitter.NoJitter
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

/**
 * Runs [task] until it produces a result that [retryOn] accepts or attempts run out.
 *
 * Designed for suspending code. Use [retryBlocking] for non-suspending code.
 * Coroutine cancellation is always propagated.
 * [CancellationException] is never retried.
 */
public suspend fun <T> retry(
    maxAttempts: Int = Int.MAX_VALUE,
    backoff: Backoff = NoBackoff,
    jitter: Jitter = NoJitter,
    retryOn: RetryOn<T> = RetryOn.default(),
    onRetryAttempt: suspend (RetryEvent<T>) -> Unit = {},
    task: suspend (RetryContext) -> T,
): T {
    require(maxAttempts > 0) {
        "maxAttempts must be greater than zero."
    }

    var lastAppliedDelay: Duration? = null
    var attempt = 1

    while (true) {
        currentCoroutineContext().ensureActive()

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
            throw RetryStoppedException(
                reason = RetryStoppedReason.MaxAttemptsReached(maxAttempts),
                lastOutcome = outcome,
            )
        }

        val backoffContext = BackoffContext(attempt, lastAppliedDelay)

        val rawDelay = backoff.nextDelay(backoffContext)
        requireValidDelay(rawDelay, "backoff")

        val appliedDelay = jitter.apply(rawDelay)
        requireValidDelay(appliedDelay, "jitter")

        val retryPlan = RetryPlan(appliedDelay)
        val retryEvent = RetryEvent(outcome, retryContext, retryPlan)

        currentCoroutineContext().ensureActive()
        onRetryAttempt(retryEvent)

        delay(appliedDelay)

        lastAppliedDelay = appliedDelay
        attempt++
    }
}

/**
 * Blocking version of [retry]. Runs [task] until [retryOn] accepts its result or attempts run out.
 *
 * Designed for blocking code. Use [retry] for suspending code.
 * [CancellationException] is never retried.
 */
public fun <T> retryBlocking(
    maxAttempts: Int = Int.MAX_VALUE,
    backoff: Backoff = NoBackoff,
    jitter: Jitter = NoJitter,
    retryOn: RetryOn<T> = RetryOn.default(),
    onRetryAttempt: (RetryEvent<T>) -> Unit = {},
    task: (RetryContext) -> T
): T {
    require(maxAttempts > 0) {
        "maxAttempts must be greater than zero."
    }

    var lastAppliedDelay: Duration? = null
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
            throw RetryStoppedException(
                reason = RetryStoppedReason.MaxAttemptsReached(maxAttempts),
                lastOutcome = outcome,
            )
        }

        val backoffContext = BackoffContext(attempt, lastAppliedDelay)

        val rawDelay = backoff.nextDelay(backoffContext)
        requireValidDelay(rawDelay, "backoff")

        val appliedDelay = jitter.apply(rawDelay)
        requireValidDelay(appliedDelay, "jitter")

        val retryPlan = RetryPlan(appliedDelay)
        val retryEvent = RetryEvent(outcome, retryContext, retryPlan)

        onRetryAttempt(retryEvent)

        sleep(appliedDelay)

        lastAppliedDelay = appliedDelay
        attempt++
    }
}

private fun requireValidDelay(delay: Duration, source: String) {
    require(delay >= Duration.ZERO && delay.isFinite()) {
        "$source delay must be finite and non-negative."
    }
}
