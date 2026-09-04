package io.github.straxess.retrykt.listener

import io.github.straxess.retrykt.RetryEvent

/**
 * Listens for retry lifecycle events.
 *
 * @see RetryEvent
 */
public interface RetryListener {

    /**
     * Called after an attempt is selected for retry and before the retry delay.
     */
    public fun onRetry(retryEvent: RetryEvent<*>) {}

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
            onRetry: ((RetryEvent<*>) -> Unit)? = null,
            onSuccess: ((RetryEvent<*>) -> Unit)? = null,
            onFailure: ((RetryEvent<*>) -> Unit)? = null,
        ): RetryListener = object : RetryListener {

            override fun onRetry(retryEvent: RetryEvent<*>) {
                onRetry?.invoke(retryEvent)
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
