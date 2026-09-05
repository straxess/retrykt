package io.github.straxess.retrykt.internal

import kotlin.time.Duration

internal fun requireFiniteNonNegative(delay: Duration, name: String) {
    require(delay >= Duration.ZERO && delay.isFinite()) {
        "$name must be finite and non-negative."
    }
}
