package io.github.straxess.retrykt.internal

import kotlin.time.Duration

internal actual fun sleepInternal(duration: Duration) {
    val millis = duration.inWholeMilliseconds
    val nanos = (duration.inWholeNanoseconds % 1_000_000).toInt()

    Thread.sleep(millis, nanos)
}
