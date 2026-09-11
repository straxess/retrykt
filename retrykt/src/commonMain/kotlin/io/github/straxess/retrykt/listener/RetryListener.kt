package io.github.straxess.retrykt.listener

/**
 * Receives retry, success, and terminal failure events.
 *
 * Callbacks run synchronously. An exception from a callback stops the operation and is passed to the caller unchanged.
 */
public interface RetryListener {

    /**
     * Called before waiting for the next attempt.
     *
     * @param attemptEvent The completed attempt.
     * @param retryPlan The delay before the next attempt.
     */
    public fun onRetry(attemptEvent: AttemptEvent<*>, retryPlan: RetryPlan) {}

    /**
     * Called when a returned value is accepted.
     */
    public fun onSuccess(attemptEvent: AttemptEvent<*>) {}

    /**
     * Called when retrying ends because an exception is not retryable or all allowed attempts are used.
     */
    public fun onFailure(attemptEvent: AttemptEvent<*>) {}

    public companion object {

        /**
         * Creates a listener from optional callbacks.
         */
        public operator fun invoke(
            onRetry: ((AttemptEvent<*>, RetryPlan) -> Unit)? = null,
            onSuccess: ((AttemptEvent<*>) -> Unit)? = null,
            onFailure: ((AttemptEvent<*>) -> Unit)? = null,
        ): RetryListener = object : RetryListener {
            override fun onRetry(attemptEvent: AttemptEvent<*>, retryPlan: RetryPlan) {
                onRetry?.invoke(attemptEvent, retryPlan)
            }

            override fun onSuccess(attemptEvent: AttemptEvent<*>) {
                onSuccess?.invoke(attemptEvent)
            }

            override fun onFailure(attemptEvent: AttemptEvent<*>) {
                onFailure?.invoke(attemptEvent)
            }
        }
    }
}
