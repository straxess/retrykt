# RetryKt

> A lightweight Kotlin Multiplatform retry library with pluggable backoff strategies.

### Features:

- Kotlin Multiplatform
- Coroutine-first API
- Blocking and suspend APIs
- JVM / Android / iOS / macOS / Linux
- Pluggable Backoff
- Pluggable Jitter
- Retry predicates
- Retry context
- Zero reflection
- Zero dependencies

---

## Installation

### Gradle

```kotlin
dependencies {
    implementation("io.github.straxess:retrykt:<version>")
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.straxess</groupId>
    <artifactId>retrykt</artifactId>
    <version>...</version>
</dependency>
```

---

# Usage

Retry an HTTP request:

```kotlin
val user = retry {
    api.getUser()
}
```

Retry only network failures:

```kotlin
val user = retry(
    retryIf = { it is IOException }
) {
    api.getUser()
}
```

Retry with exponential backoff:

```kotlin
val user = retry(
    maxAttempts = 5,
    backoff = ExponentialBackoff(
        initialDelay = 100.milliseconds,
        stepMultiplier = 2.0
    )
) {
    api.getUser()
}
```

Use retry context:

```kotlin
retry(maxAttempts = 5) {
    logger.info("Attempt #${it.attempt}")
    uploadFile()
}
```

Or inspect the previous failure:

```kotlin
retry { ctx ->
    ctx.lastThrowable?.let {
        logger.warn(it) { "Retrying..." }
    }

    connect()
}
```

---

# Backoff Strategies

Built-in implementations:

```kotlin
NoBackoff(default)
ConstantBackoff
LinearBackoff
ExponentialBackoff
```

All strategies support custom jitter.

---

# Custom Backoff

```kotlin
class FibonacciBackoff : Backoff {
    override fun nextDelay(attempt: Int): Duration {
        // ...
    }
}
```

Then:

```kotlin
retry(backoff = FibonacciBackoff()) {
    sync()
}
```

---

# Jitter

Randomize delays to avoid synchronized retries.

```kotlin
ExponentialBackoff(
    initialDelay = 200.milliseconds,
    jitter = RandomJitter(100.milliseconds)
)
```

Or implement your own:

```kotlin
class MyJitter : Jitter {
    override fun apply(delay: Duration): Duration {
        // ...
    }
}
```

---

# Retry Conditions

Retry only selected exceptions.

```kotlin
retry(retryIf = { it is IOException || it is TimeoutException }) {
    request()
}
```

---

# Retry Context

Every retry execution has access to:

```kotlin
attempt       // Current attempt number.
lastThrowable // The exception from the previous attempt.
```

---

# Cancellation

`CancellationException` is never retried.

Coroutine cancellation always propagates immediately.

---

# License

Apache License 2.0
