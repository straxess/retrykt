package io.github.straxess.retrykt.internal

import kotlin.time.Duration

internal actual fun sleep(duration: Duration) {
    require(!duration.isNegative())

    if (duration == Duration.ZERO) {
        return
    }

    val millis = duration.inWholeMilliseconds
    val nanos = (duration.inWholeNanoseconds % 1_000_000).toInt()

    Thread.sleep(millis, nanos)
}
