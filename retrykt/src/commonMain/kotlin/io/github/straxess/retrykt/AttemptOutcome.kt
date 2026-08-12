package io.github.straxess.retrykt

/**
 * Represents the outcome of a single task invocation.
 *
 * It either returned a value or threw an exception. [RetryOn] decides whether to try again.
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
