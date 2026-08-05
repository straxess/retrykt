package io.github.straxess.retrykt

public class RetryOn<in T> internal constructor(
    internal val shouldRetry: (AttemptOutcome<T>) -> Boolean,
) {

    public companion object {

        public fun <T> standard(): RetryOn<T> = thrown { true }

        public fun <T> thrown(predicate: (Throwable) -> Boolean): RetryOn<T> = RetryOn { outcome ->
            when (outcome) {
                is AttemptOutcome.Returned -> false
                is AttemptOutcome.Thrown -> predicate(outcome.throwable)
            }
        }

        public fun <T> returned(predicate: (T) -> Boolean): RetryOn<T> = RetryOn { outcome ->
            when (outcome) {
                is AttemptOutcome.Returned -> predicate(outcome.value)
                is AttemptOutcome.Thrown -> false
            }
        }

        public fun <T> outcome(predicate: (AttemptOutcome<T>) -> Boolean): RetryOn<T> = RetryOn(predicate)
    }
}
