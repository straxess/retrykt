package io.github.straxess.retrykt

/**
 * Event emitted immediately before waiting for the next retry attempt.
 *
 * Contains the outcome of the completed attempt,
 * the current retry [context], and the [plan] for the next attempt.
 */
public class RetryEvent<T> internal constructor(
    public val outcome: AttemptOutcome<T>,
    public val context: RetryContext,
    public val plan: RetryPlan,
)
