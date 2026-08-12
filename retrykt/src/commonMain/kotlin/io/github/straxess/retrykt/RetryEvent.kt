package io.github.straxess.retrykt

/**
 * Sent just before waiting to run another attempt.
 *
 * It includes the last [outcome], its [context], and the [plan] for the next attempt.
 */
public class RetryEvent<T> internal constructor(
    public val outcome: AttemptOutcome<T>,
    public val context: RetryContext,
    public val plan: RetryPlan,
)
