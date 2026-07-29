package io.github.straxess.retrykt

import java.util.concurrent.TimeUnit
import kotlin.time.Duration

internal actual fun sleep(duration: Duration) {
    TimeUnit.MILLISECONDS.sleep(duration.inWholeMilliseconds)
}
