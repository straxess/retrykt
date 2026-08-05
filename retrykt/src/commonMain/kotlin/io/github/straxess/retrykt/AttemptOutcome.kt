package io.github.straxess.retrykt

/**
 * Represents the outcome of a single task invocation.
 *
 * A task can only:
 * - return a value;
 * - throw a throwable.
 *
 * This type intentionally models Kotlin execution semantics rather than business success or failure.
 * Whether an outcome should trigger another attempt is determined by [RetryOn].
 */
public sealed interface AttemptOutcome<out T> {

    /**
     * The task completed normally and returned a [value].
     */
    public class Returned<T> internal constructor(
        public val value: T,
    ) : AttemptOutcome<T>

    /**
     * The task terminated by throwing a [throwable].
     */
    public class Thrown internal constructor(
        public val throwable: Throwable,
    ) : AttemptOutcome<Nothing>
}
