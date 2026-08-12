package io.github.straxess.retrykt.internal

import platform.windows.Sleep
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A positive [duration] less than 1 ms is rounded up to 1 ms
 * because `Sleep(0)` does not guarantee that the current thread will wait.
 */
internal actual fun sleep(duration: Duration) {
    require(!duration.isNegative())

    if (duration == Duration.ZERO) {
        return
    }

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
