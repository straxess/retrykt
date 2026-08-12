package io.github.straxess.retrykt.internal

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.EINTR
import platform.posix.errno
import platform.posix.nanosleep
import platform.posix.timespec
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@OptIn(ExperimentalForeignApi::class)
internal actual fun sleep(duration: Duration) {
    require(!duration.isNegative())

    if (duration == Duration.ZERO) {
        return
    }

    val mark = TimeSource.Monotonic.markNow()
    var remainingDuration = duration

    memScoped {
        val request = alloc<timespec>()

        while (true) {
            val seconds = remainingDuration.inWholeSeconds
            val nanos = (remainingDuration - seconds.seconds).inWholeNanoseconds
            request.tv_sec = seconds
            request.tv_nsec = nanos

            if (nanosleep(request.ptr, null) == 0) {
                return
            }

            check(errno == EINTR) { "nanosleep failed with errno $errno." }

            remainingDuration = duration - mark.elapsedNow()
            if (remainingDuration <= Duration.ZERO) {
                return
            }
        }
    }
}
