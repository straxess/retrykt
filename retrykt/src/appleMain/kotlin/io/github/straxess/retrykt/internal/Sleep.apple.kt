package io.github.straxess.retrykt.internal

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import platform.posix.nanosleep
import platform.posix.timespec
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalForeignApi::class)
internal actual fun sleep(duration: Duration) {
    require(!duration.isNegative())

    if (duration == Duration.ZERO) {
        return
    }

    val seconds = duration.inWholeSeconds
    val nanos = (duration - seconds.seconds).inWholeNanoseconds
    val request = cValue<timespec> {
        tv_sec = seconds
        tv_nsec = nanos
    }

    nanosleep(request, null)
}
