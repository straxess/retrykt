package io.github.straxess.retrykt

/**
 * Decides whether to retry an [AttemptOutcome].
 *
 * Use [default], [thrown], [returned], or [outcome] to create a policy. An exception from a predicate is passed to the
 * caller unchanged.
 */
public class RetryOn<in T> internal constructor(
    internal val shouldRetry: (AttemptOutcome<T>) -> Boolean,
) {

    public companion object {

        /**
         * Retries any thrown exception except [Error]. Returned values are accepted.
         */
        public fun <T> default(): RetryOn<T> = thrown { it !is Error }

        /**
         * Retries a thrown exception when [predicate] returns `true`. Returned values are accepted.
         */
        public fun <T> thrown(predicate: (Throwable) -> Boolean): RetryOn<T> = RetryOn { outcome ->
            when (outcome) {
                is AttemptOutcome.Returned -> false
                is AttemptOutcome.Thrown -> predicate(outcome.throwable)
            }
        }

        /**
         * Retries a returned value when [predicate] returns `true`. Thrown exceptions are not retried.
         */
        public fun <T> returned(predicate: (T) -> Boolean): RetryOn<T> = RetryOn { outcome ->
            when (outcome) {
                is AttemptOutcome.Returned -> predicate(outcome.value)
                is AttemptOutcome.Thrown -> false
            }
        }

        /**
         * Retries when [predicate] returns `true` for a returned value or thrown exception.
         */
        public fun <T> outcome(predicate: (AttemptOutcome<T>) -> Boolean): RetryOn<T> = RetryOn(predicate)
    }
}
