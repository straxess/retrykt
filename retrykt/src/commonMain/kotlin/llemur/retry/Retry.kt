package llemur.retry

import kotlinx.coroutines.delay
import llemur.retry.backoff.Backoff
import llemur.retry.backoff.ConstantBackoff
import kotlin.time.Duration

class Retry(
    val maxTries: Int = Int.MAX_VALUE,
    val backoff: Backoff = ConstantBackoff(Duration.ZERO),
) {

    fun <T> execute(task: (RetryContext) -> T): T {
        var attempt = 0
        while (attempt < maxTries) {
            try {
                val result = task(RetryContext(attempt))
                return result
            } catch (e: Exception) {
                val delay = backoff.nextDelay(attempt)
                sleep(delay)
                attempt++
            }
        }

        throw RuntimeException("Retry failed")
    }

    suspend fun <T> executeSuspend(task: suspend (RetryContext) -> T): T {
        var attempt = 0
        while (attempt < maxTries) {
            try {
                attempt++
                val result = task(RetryContext(attempt))
                return result
            } catch (e: Exception) {
                val delay = backoff.nextDelay(attempt)
                delay(delay)
                attempt++
            }
        }

        throw RuntimeException("Retry failed")
    }
}

data class RetryContext(
    val attempt: Int
)

/**
 * Blocking sleep for non-suspend tasks
 */
expect fun sleep(duration: Duration)
