package io.github.straxess.retrykt.listener

/**
 * Listens for retry lifecycle events.
 *
 * @see RetryEvent
 */
public interface RetryListener {

    /**
     * Called when an attempt will be retried, before the retry delay is applied.
     *
     * @param retryEvent Event describing the completed attempt.
     * @param retryDecision Decision describing the next retry attempt.
     */
    public fun onRetry(retryEvent: RetryEvent<*>, retryDecision: RetryDecision) {}

    /**
     * Called when the retry operation succeeds.
     */
    public fun onSuccess(retryEvent: RetryEvent<*>) {}

    /**
     * Called when the retry operation fails and will not be retried.
     */
    public fun onFailure(retryEvent: RetryEvent<*>) {}

    public companion object {

        /**
         * Creates a [RetryListener] from the given callbacks.
         */
        public operator fun invoke(
            onRetry: ((RetryEvent<*>, RetryDecision) -> Unit)? = null,
            onSuccess: ((RetryEvent<*>) -> Unit)? = null,
            onFailure: ((RetryEvent<*>) -> Unit)? = null,
        ): RetryListener = object : RetryListener {

            override fun onRetry(retryEvent: RetryEvent<*>, retryDecision: RetryDecision) {
                onRetry?.invoke(retryEvent, retryDecision)
            }

            override fun onSuccess(retryEvent: RetryEvent<*>) {
                onSuccess?.invoke(retryEvent)
            }

            override fun onFailure(retryEvent: RetryEvent<*>) {
                onFailure?.invoke(retryEvent)
            }
        }
    }
}
