package io.github.straxess.retrykt

/**
 * Thrown when [retry] or [retryBlocking] stops retrying according to the configured retry policy.
 *
 * RetryKt creates this exception itself. Exceptions from your task, retry policy, backoff, jitter, or callback pass
 * through unchanged. If [lastOutcome] is [AttemptOutcome.Thrown], that throwable is also the [cause].
 */
public class RetryStoppedException internal constructor(
    /** Why retries stopped. */
    public val reason: RetryStoppedReason,
    /** The result of the final allowed attempt. */
    public val lastOutcome: AttemptOutcome<*>,
) : RuntimeException(
    reason.description(),
    (lastOutcome as? AttemptOutcome.Thrown)?.throwable,
)
