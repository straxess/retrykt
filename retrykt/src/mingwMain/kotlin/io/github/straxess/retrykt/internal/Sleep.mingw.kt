package io.github.straxess.retrykt.internal

import platform.windows.Sleep
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Rounds a positive [duration] below 1 ms up to 1 ms because Windows `Sleep(0)` may not wait.
 */
internal actual fun sleepInternal(duration: Duration) {
    var remaining = duration
    val maxChunk = UInt.MAX_VALUE.toLong().milliseconds

    // Windows Sleep takes a UInt millisecond count, so long waits need several calls.
    while (remaining > Duration.ZERO) {
        val chunk = remaining.coerceAtMost(maxChunk)
        val millis = chunk.inWholeMilliseconds.coerceAtLeast(1)

        Sleep(millis.toUInt())
        remaining -= chunk
    }
}
