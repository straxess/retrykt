package io.github.straxess.retrykt

/**
 * Sent just before waiting to run another attempt.
 *
 * It includes the last [outcome], and its [context].
 */
public class RetryEvent<T> internal constructor(
    public val outcome: AttemptOutcome<T>,
    public val context: RetryContext,
)
