package io.github.straxess.retrykt.internal

import kotlin.time.Duration

/**
 * Blocks the current thread for the specified [duration].
 */
internal expect fun sleep(duration: Duration)
