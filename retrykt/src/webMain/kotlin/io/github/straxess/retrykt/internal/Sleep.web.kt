package io.github.straxess.retrykt.internal

import kotlin.time.Duration

/**
 * JS and Wasm cannot block for a positive duration.
 *
 * Zero is allowed so [io.github.straxess.retrykt.retryBlocking] works with
 * [io.github.straxess.retrykt.backoff.NoBackoff].
 */
internal actual fun sleep(duration: Duration) {
    require(!duration.isNegative())

    if (duration == Duration.ZERO) {
        return
    }

    throw UnsupportedOperationException(
        "Blocking sleep is not supported for Kotlin/JS and Kotlin/Wasm"
    )
}
