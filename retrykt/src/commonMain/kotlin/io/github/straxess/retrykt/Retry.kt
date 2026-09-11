package io.github.straxess.retrykt

import io.github.straxess.retrykt.backoff.Backoff
import io.github.straxess.retrykt.backoff.BackoffContext
import io.github.straxess.retrykt.backoff.NoBackoff
import io.github.straxess.retrykt.internal.checkFiniteNonNegative
import io.github.straxess.retrykt.internal.sleep
import io.github.straxess.retrykt.jitter.Jitter
import io.github.straxess.retrykt.jitter.NoJitter
import io.github.straxess.retrykt.listener.AttemptEvent
import io.github.straxess.retrykt.listener.RetryListener
import io.github.straxess.retrykt.listener.RetryPlan
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

/**
 * Runs the suspending [task] until [retryOn] accepts its result or all [maxAttempts] are used.
 *
 * Cancellation stops the operation, including during a delay. [CancellationException] is not passed to [retryOn] or
 * retried. Exceptions from [retryOn], [backoff], [jitter], and [listener] are passed to the caller unchanged.
 *
 * @throws IllegalArgumentException if [maxAttempts] is zero or negative.
 * @throws IllegalStateException if [backoff] or [jitter] returns a negative or infinite duration.
 * @throws RetryExhaustedException if the retry policy rejects the last allowed outcome.
 */
public suspend fun <T> retry(
    maxAttempts: Int = Int.MAX_VALUE,
    backoff: Backoff = NoBackoff,
    jitter: Jitter = NoJitter,
    retryOn: RetryOn<T> = RetryOn.default(),
    listener: RetryListener = RetryListener(),
    task: suspend (RetryContext<T>) -> T,
): T {
    require(maxAttempts > 0) {
        "maxAttempts must be greater than zero."
    }

    var outcome: AttemptOutcome<T>? = null
    var prevAppliedDelay: Duration? = null
    var attempt = 1

    while (true) {
        currentCoroutineContext().ensureActive()

        val retryContext = RetryContext(attempt, maxAttempts, outcome)

        outcome = try {
            val returned = task(retryContext)
            AttemptOutcome.Returned(returned)
        } catch (t: Throwable) {
            if (t is CancellationException) {
                throw t
            }

            AttemptOutcome.Thrown(t)
        }

        currentCoroutineContext().ensureActive()

        val attemptEvent = AttemptEvent(outcome, retryContext)

        val shouldRetry = retryOn.shouldRetry(outcome)

        currentCoroutineContext().ensureActive()

        if (!shouldRetry) {
            return when (outcome) {
                is AttemptOutcome.Returned -> {
                    listener.onSuccess(attemptEvent)
                    outcome.value
                }

                is AttemptOutcome.Thrown -> {
                    listener.onFailure(attemptEvent)
                    throw outcome.throwable
                }
            }
        }

        if (attempt == maxAttempts) {
            listener.onFailure(attemptEvent)
            throw RetryExhaustedException(
                reason = RetryExhaustionReason.MaxAttemptsReached(maxAttempts),
                lastOutcome = outcome,
            )
        }

        val backoffContext = BackoffContext(attempt, prevAppliedDelay)
        val backoffDelay = backoff.calculateDelay(backoffContext)
        checkFiniteNonNegative(backoffDelay, "backoff delay")

        val nextAppliedDelay = jitter.apply(backoffDelay)
        checkFiniteNonNegative(nextAppliedDelay, "next applied delay")

        val retryPlan = RetryPlan(nextAppliedDelay = nextAppliedDelay)

        currentCoroutineContext().ensureActive()
        listener.onRetry(attemptEvent, retryPlan)

        delay(nextAppliedDelay)

        prevAppliedDelay = nextAppliedDelay
        attempt++
    }
}

/**
 * Runs the blocking [task] until [retryOn] accepts its result or all [maxAttempts] are used.
 *
 * Use [retry] when the caller can suspend. JS and Wasm support this function only with zero delay because they cannot
 * block the current thread. [CancellationException] is not passed to [retryOn] or retried. Exceptions from [retryOn],
 * [backoff], [jitter], and [listener] are passed to the caller unchanged.
 *
 * @throws IllegalArgumentException if [maxAttempts] is zero or negative.
 * @throws IllegalStateException if [backoff] or [jitter] returns a negative or infinite duration.
 * @throws RetryExhaustedException if the retry policy rejects the last allowed outcome.
 */
public fun <T> retryBlocking(
    maxAttempts: Int = Int.MAX_VALUE,
    backoff: Backoff = NoBackoff,
    jitter: Jitter = NoJitter,
    retryOn: RetryOn<T> = RetryOn.default(),
    listener: RetryListener = RetryListener(),
    task: (RetryContext<T>) -> T,
): T {
    require(maxAttempts > 0) {
        "maxAttempts must be greater than zero."
    }

    var outcome: AttemptOutcome<T>? = null
    var prevAppliedDelay: Duration? = null
    var attempt = 1

    while (true) {
        val retryContext = RetryContext(attempt, maxAttempts, outcome)

        outcome = try {
            val returned = task(retryContext)
            AttemptOutcome.Returned(returned)
        } catch (t: Throwable) {
            if (t is CancellationException) {
                throw t
            }

            AttemptOutcome.Thrown(t)
        }

        val attemptEvent = AttemptEvent(outcome, retryContext)

        if (!retryOn.shouldRetry(outcome)) {
            return when (outcome) {
                is AttemptOutcome.Returned -> {
                    listener.onSuccess(attemptEvent)
                    outcome.value
                }

                is AttemptOutcome.Thrown -> {
                    listener.onFailure(attemptEvent)
                    throw outcome.throwable
                }
            }
        }

        if (attempt == maxAttempts) {
            listener.onFailure(attemptEvent)
            throw RetryExhaustedException(
                reason = RetryExhaustionReason.MaxAttemptsReached(maxAttempts),
                lastOutcome = outcome,
            )
        }

        val backoffContext = BackoffContext(attempt, prevAppliedDelay)
        val backoffDelay = backoff.calculateDelay(backoffContext)
        checkFiniteNonNegative(backoffDelay, "backoff delay")

        val nextAppliedDelay = jitter.apply(backoffDelay)
        checkFiniteNonNegative(nextAppliedDelay, "next applied delay")

        val retryPlan = RetryPlan(nextAppliedDelay = nextAppliedDelay)

        listener.onRetry(attemptEvent, retryPlan)

        sleep(nextAppliedDelay)

        prevAppliedDelay = nextAppliedDelay
        attempt++
    }
}
