# RetryKt

> A lightweight Kotlin Multiplatform retry library with pluggable backoff strategies.

## Features

- Kotlin Multiplatform
- Coroutine-first API
- `retry()` and `retryBlocking()`
- JVM / Android / iOS / macOS / Linux
- Pluggable Backoff
- Pluggable Jitter
- Retry predicates
- Retry context
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

## Quick Start

> **Rule of thumb**
>
> Use `retry()` in suspend code.  
> Use `retryBlocking()` everywhere else.

Retry an HTTP request:

```kotlin
val user = retry {
    api.getUser()
}
```

Retry only network failures:

```kotlin
val user = retry(retryIf = { it is IOException }) {
    api.getUser()
}
```

Retry with exponential backoff:

```kotlin
val user = retry(
    maxAttempts = 5,
    backoff = ExponentialBackoff(
        initialDelay = 100.milliseconds,
        stepMultiplier = 2.0,
    )
) {
    api.getUser()
}
```

Use the retry context:

```kotlin
retry(maxAttempts = 3) { ctx ->
    logger.info("Attempt #${ctx.attempt}")

    ctx.lastThrowable?.let {
        logger.warn(it) { "Previous attempt failed" }
    }

    uploadFile()
}
```

---

## Why RetryKt?

Both `retry()` and `retryBlocking()` provide the same capabilities:

- Retry context (`attempt`, `lastThrowable`)
- Built-in backoff strategies
- Support of custom backoff implementations
- Pluggable jitter
- Retry predicates
- Coroutine cancellation support

---

## Backoff

Built-in implementations:

```kotlin
NoBackoff          // 0ms
ConstantBackoff    // 100ms, 100ms, 100ms
LinearBackoff      // 100ms, 200ms, 300ms
ExponentialBackoff // 100ms, 200ms, 400ms
```

Custom implementations are also supported.

```kotlin
class FibonacciBackoff : Backoff {

    override fun nextDelay(attempt: Int): Duration {
        // ...
    }
}
```

```kotlin
retry(backoff = FibonacciBackoff()) {
    task()
}
```

---

## Jitter

Avoid synchronized retries by randomizing delays.

```kotlin
LinearBackoff(
    step = 200.milliseconds,
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

## Retry Predicates

Retry only selected exceptions.

```kotlin
retry(retryIf = { it is IOException || it is TimeoutException }) {
    request()
}
```

---

## Coroutines

Use `retry()` in coroutine-based code.

### Ktor Client

```kotlin
val user = retry(retryIf = { it is IOException }) {
    client.get("/users/$id").body<User>()
}
```

### Repository

```kotlin
class UserRepository(private val api: UserApi) {

    suspend fun getUser(id: Long): User {
        return retry(backoff = LinearBackoff(200.milliseconds)) {
            api.getUser(id)
        }
    }
}
```

### Flow

```kotlin
flow {
    emit(
        retry {
            api.loadConfiguration()
        }
    )
}
```

Typical use cases include:

| Platform             | Examples                                                 |
|----------------------|----------------------------------------------------------|
| Kotlin Multiplatform | Shared business logic                                    |
| Android              | ViewModel, Repository, DataStore                         |
| Server               | Ktor, suspend services, coroutine-based database clients |
| Desktop              | Compose Multiplatform                                    |
| CLI                  | Coroutine-based tools and scripts                        |

---

## Blocking Code

Use `retryBlocking()` when the execution context is synchronous and a `suspend` function cannot be called.

### Caffeine CacheLoader (JVM)

```kotlin
val cache = Caffeine.newBuilder()
    .build<String, User> { id ->
        retryBlocking {
            api.loadUser(id)
        }
    }
```

The callback signature is defined by the library and cannot be `suspend`.

### Android WorkManager

```kotlin
class SyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        retryBlocking {
            uploadPendingFiles()
        }

        return Result.success()
    }
}
```

The callback signature is defined by the Android framework and cannot be `suspend`.

### Kotlin/Native C callback

```kotlin
// Pseudo code

val callback = staticCFunction { chunk ->

    retryBlocking {
        api.send(chunk)
    }
}
```

The callback signature is defined by the native library and cannot be `suspend`.

Typical use cases include:

| Platform             | Examples                                           |
|----------------------|----------------------------------------------------|
| JVM                  | Cache loaders, HTTP clients, JDBC, file I/O        |
| Android              | WorkManager, Binder services, blocking Room DAOs   |
| Kotlin/Native        | C callbacks, POSIX APIs, platform SDKs             |
| Kotlin Multiplatform | File systems, embedded databases, synchronous SDKs |
| CLI/Desktop          | Configuration files, external processes            |

---

## Coroutine Cancellation

`CancellationException` is never retried and is always propagated immediately.

---

## License

Apache License 2.0
