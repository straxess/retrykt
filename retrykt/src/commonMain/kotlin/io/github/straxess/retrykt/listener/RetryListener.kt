package io.github.straxess.retrykt.listener

/**
 * Receives retry, success, and failure events.
 *
 * Callbacks run synchronously. An exception from a callback stops the operation and is passed to the caller unchanged.
 */
public interface RetryListener {

    /**
     * Called before waiting for the next attempt.
     *
     * @param retryEvent The completed attempt.
     * @param retryDecision The delay before the next attempt.
     */
    public fun onRetry(retryEvent: RetryEvent<*>, retryDecision: RetryDecision) {}

    /**
     * Called when a returned value is accepted.
     */
    public fun onSuccess(retryEvent: RetryEvent<*>) {}

    /**
     * Called when an exception will not be retried or no attempts remain.
     */
    public fun onFailure(retryEvent: RetryEvent<*>) {}

    public companion object {

        /**
         * Creates a listener from optional callbacks.
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
