package io.github.straxess.retrykt.internal

import platform.windows.Sleep
import kotlin.time.Duration

/**
 * A positive [duration] less than 1 ms is rounded up to 1 ms
 * because `Sleep(0)` does not guarantee that the current thread will wait.
 */
internal actual fun sleep(duration: Duration) {
    require(!duration.isNegative())

    if (duration == Duration.ZERO) {
        return
    }

    val millis = duration.inWholeMilliseconds
        .coerceAtLeast(1)
        .coerceAtMost(UInt.MAX_VALUE.toLong())

    Sleep(millis.toUInt())
}
