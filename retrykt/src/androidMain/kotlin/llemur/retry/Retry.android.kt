package llemur.retry

import java.util.concurrent.TimeUnit
import kotlin.time.Duration

actual fun sleep(duration: Duration) {
    TimeUnit.MILLISECONDS.sleep(duration.inWholeMilliseconds)
}
