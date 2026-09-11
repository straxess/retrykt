package io.github.straxess.retrykt.internal

import platform.windows.Sleep
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal const val MAX_FINITE_SLEEP_MILLIS: Long = 0xFFFF_FFFEL

/**
 * Rounds a positive [duration] below 1 ms up to 1 ms because Windows `Sleep(0)` may not wait.
 */
internal actual fun sleepInternal(duration: Duration) {
    var remaining = duration
    val maxChunk = MAX_FINITE_SLEEP_MILLIS.milliseconds

    // UInt.MAX_VALUE means INFINITE to Windows Sleep, so finite long waits need smaller chunks.
    while (remaining > Duration.ZERO) {
        val chunk = remaining.coerceAtMost(maxChunk)
        val millis = chunk.inWholeMilliseconds.coerceAtLeast(1)

        Sleep(millis.toUInt())
        remaining -= chunk
    }
}
