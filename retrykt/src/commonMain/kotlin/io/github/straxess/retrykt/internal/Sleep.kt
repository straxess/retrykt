package io.github.straxess.retrykt.internal

import kotlin.time.Duration

/**
 * Blocks the current thread for [duration].
 */
internal expect fun sleep(duration: Duration)
