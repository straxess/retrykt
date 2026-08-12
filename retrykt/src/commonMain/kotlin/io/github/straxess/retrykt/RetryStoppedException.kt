package io.github.straxess.retrykt

/**
 * Thrown when [retry] or [retryBlocking] stops retrying according to the configured retry policy.
 *
 * This exception is produced only by RetryKt itself.
 * Exceptions thrown by user callbacks, such as RetryOn predicates or lifecycle callbacks, are propagated unchanged.
 * When [lastOutcome] is [AttemptOutcome.Thrown], its throwable is available as this exception's [cause].
 */
public class RetryStoppedException internal constructor(
    public val reason: RetryStoppedReason,
    public val lastOutcome: AttemptOutcome<*>,
) : RuntimeException(
    reason.description(),
    (lastOutcome as? AttemptOutcome.Thrown)?.throwable,
)
