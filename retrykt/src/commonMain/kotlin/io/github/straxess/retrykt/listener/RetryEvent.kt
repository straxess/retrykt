package io.github.straxess.retrykt.listener

import io.github.straxess.retrykt.AttemptOutcome
import io.github.straxess.retrykt.RetryContext

/**
 * The outcome and context of a completed attempt.
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
 * @param outcome The completed attempt's outcome.
 * @param context The context used for that attempt.
 */
public class RetryEvent<T> internal constructor(
    public val outcome: AttemptOutcome<T>,
    public val context: RetryContext<T>,
)
