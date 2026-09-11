package io.github.straxess.retrykt

/**
 * Thrown when [retry] or [retryBlocking] uses all allowed attempts and the last result is still retryable.
 *
 * Other exceptions are passed to the caller unchanged. If [lastOutcome] is [AttemptOutcome.Thrown], its exception is
 * also available as [cause].
 */
public class RetryStoppedException internal constructor(

    /**
     * Why RetryKt stopped retrying.
     */
    public val reason: RetryStoppedReason,

    /**
     * The outcome of the last allowed attempt.
     */
    public val lastOutcome: AttemptOutcome<*>,
) : RuntimeException(
    reason.description(),
    (lastOutcome as? AttemptOutcome.Thrown)?.throwable,
)
