package llemur.retry

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import platform.posix.nanosleep
import platform.posix.timespec
import kotlin.time.Duration

@OptIn(ExperimentalForeignApi::class)
actual fun sleep(duration: Duration) {
    val millis = duration.inWholeMilliseconds

    val request = cValue<timespec> {
        tv_sec = millis / 1000
        tv_nsec = (millis % 1000) * 1_000_000
    }

    nanosleep(request, null)
}
