package io.github.straxess.retrykt

/**
 * The result of one attempt: a returned value or a thrown exception.
 * [RetryOn] uses this result to decide whether to retry.
 */
public sealed interface AttemptOutcome<out T> {

    /**
     * An attempt that returned [value].
     */
    public class Returned<T> internal constructor(
        public val value: T,
    ) : AttemptOutcome<T>

    /**
     * An attempt that threw [throwable].
     */
    public class Thrown internal constructor(
        public val throwable: Throwable,
    ) : AttemptOutcome<Nothing>
}
