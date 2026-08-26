package io.github.straxess.retrykt

/**
 * Describes the result of a completed task attempt and its context.
 *
 * ```
 * Attempt ──> outcome ──┬──> onSuccess
 *                       ├──> onFailure
 *                       └──> onRetry
 *                              │
 *                              ▼
 *                         RetryEvent
 *                         ├─ outcome (current attempt)
 *                         └─ context
 *                            └─ prevOutcome (previous attempt)
 * ```
 *
 * @param [outcome] result of the current attempt.
 * @param [context] context in which the attempt was executed.
 */
public class RetryEvent<T> internal constructor(
    public val outcome: AttemptOutcome<T>,
    public val context: RetryContext<T>,
)
