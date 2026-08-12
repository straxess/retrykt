package io.github.straxess.retrykt

/**
 * Decides whether RetryKt should run another attempt after an outcome.
 *
 * Create one with [default], [thrown], [returned], or [outcome].
 */
public class RetryOn<in T> internal constructor(
    internal val shouldRetry: (AttemptOutcome<T>) -> Boolean,
) {

    public companion object {

        /**
         * Retries only [Throwable], excluding [Error].
         */
        public fun <T> default(): RetryOn<T> = thrown { it !is Error }

        /**
         * Retries thrown exceptions when [predicate] returns `true`.
         * Returned values are accepted.
         */
        public fun <T> thrown(predicate: (Throwable) -> Boolean): RetryOn<T> = RetryOn { outcome ->
            when (outcome) {
                is AttemptOutcome.Returned -> false
                is AttemptOutcome.Thrown -> predicate(outcome.throwable)
            }
        }

        /**
         * Retries returned values when [predicate] returns `true`.
         * Thrown exceptions are propagated.
         */
        public fun <T> returned(predicate: (T) -> Boolean): RetryOn<T> = RetryOn { outcome ->
            when (outcome) {
                is AttemptOutcome.Returned -> predicate(outcome.value)
                is AttemptOutcome.Thrown -> false
            }
        }

        /**
         * Retries when [predicate] returns `true` for either kind of outcome.
         */
        public fun <T> outcome(predicate: (AttemptOutcome<T>) -> Boolean): RetryOn<T> = RetryOn(predicate)
    }
}
