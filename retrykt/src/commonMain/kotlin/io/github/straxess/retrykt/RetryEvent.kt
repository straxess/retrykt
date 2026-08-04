package io.github.straxess.retrykt

/**
 * Event emitted after a failed attempt when another retry will be performed.
 *
 * Contains the current retry [context] and the [plan] for the next attempt.
 */
public class RetryEvent internal constructor(
    public val context: RetryContext,
    public val plan: RetryPlan,
)
