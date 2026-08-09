package io.github.straxess.retrykt.internal

import kotlin.time.Duration

/**
 * Blocking sleep with a positive duration is not available on Kotlin/JS and Kotlin/Wasm.
 *
 * Zero duration is supported to allow using [io.github.straxess.retrykt.retryBlocking]
 * with [io.github.straxess.retrykt.backoff.NoBackoff].
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
