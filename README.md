# RetryKt

> A lightweight Kotlin Multiplatform retry library with pluggable backoff strategies.

```kotlin
val result = retry {
    api.call()
}
```

## Why RetryKt?

RetryKt provides a consistent retry model across Kotlin Multiplatform.

Whether your code is coroutine-based or synchronous, the same concepts and configuration apply.

- One retry model for both `retry()` and `retryBlocking()`
- First-class Kotlin Multiplatform support
- Supports JVM, Android, Apple, Linux, Windows, JavaScript, and WebAssembly
- Fully customizable backoff and jitter implementations
- Retry context available on every attempt
- Zero dependencies

## Features

- Kotlin Multiplatform
- `retry()` and `retryBlocking()`
- Coroutine and blocking APIs
- Built-in and custom backoff implementations
- Built-in and custom jitter implementations
- Custom retry predicates
- Retry context (`attempt`, `lastThrowable`)
- Coroutine cancellation support
- Zero dependencies

## Supported Platforms

RetryKt is built with Kotlin Multiplatform and supports:

- JVM
- Android
- iOS
- macOS
- Linux
- Windows (MinGW)
- JavaScript
- WebAssembly (WasmJs)

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
val user = retry(shouldRetry = { it is IOException }) {
    api.getUser()
}
```

Retry with exponential backoff:

```kotlin
val user = retry(
    maxAttempts = 5,
    backoff = ExponentialBackoff(
        initialDelay = 100.milliseconds,
        multiplier = 2.0,
        maxDelay = 10.seconds,
    )
) {
    api.getUser()
}
```

Use the retry context:

```kotlin
retry(maxAttempts = 3) { ctx ->
    log.info("Attempt ${ctx.attempt}/${ctx.maxAttempts}")

    ctx.lastThrowable?.let {
        log.warn(it) { "Previous attempt failed" }
    }

    uploadFile()
}
```

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
    increment = 200.milliseconds,
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
retry(shouldRetry = { it is IOException || it is TimeoutException }) {
    request()
}
```

---

## Coroutines

Use `retry()` in coroutine-based code.

### Ktor Client

```kotlin
val user = retry(shouldRetry = { it is IOException }) {
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
