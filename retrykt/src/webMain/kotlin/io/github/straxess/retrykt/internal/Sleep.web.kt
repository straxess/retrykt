package io.github.straxess.retrykt.internal

import kotlin.time.Duration

/**
 * Always fails because JS and Wasm cannot block the current thread.
 * Zero delays are handled before this function is called.
 */
internal actual fun sleepInternal(duration: Duration): Unit = throw UnsupportedOperationException(
    "Blocking sleep is not supported for Kotlin/JS and Kotlin/Wasm",
)
