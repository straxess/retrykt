package io.github.straxess.retrykt

/**
 * Thrown when [retry] or [retryBlocking] uses all allowed attempts and the retry policy still rejects the outcome.
 *
 * Other exceptions are passed to the caller unchanged. If [lastOutcome] is [AttemptOutcome.Thrown], its exception is
 * also available as [cause].
 */
public class RetryExhaustedException internal constructor(

    /**
     * Why RetryKt exhausted the retry process.
     */
    public val reason: RetryExhaustionReason,

    /**
     * The outcome of the last allowed attempt.
     */
    public val lastOutcome: AttemptOutcome<*>,
) : RuntimeException(
    reason.description(),
    (lastOutcome as? AttemptOutcome.Thrown)?.throwable,
)
