# RetryKt

[![Maven Central](https://img.shields.io/maven-central/v/io.github.straxess/retrykt)](https://central.sonatype.com/artifact/io.github.straxess/retrykt)
[![Build](https://github.com/straxess/retrykt/actions/workflows/gradle.yml/badge.svg)](https://github.com/straxess/retrykt/actions/workflows/gradle.yml)
[![codecov](https://codecov.io/gh/straxess/retrykt/graph/badge.svg)](https://codecov.io/gh/straxess/retrykt)
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![License](https://img.shields.io/github/license/straxess/retrykt)](LICENSE)

> A lightweight Kotlin Multiplatform retry library with coroutine and blocking APIs.

RetryKt provides a retry model across Kotlin platforms with retry policies, configurable backoff strategies, jitter, and
a minimal runtime footprint.

RetryKt intentionally focuses on reliable retries instead of providing a complete resilience framework.

```kotlin
val user = retry {
    api.getUser()
}

val response = retry(
    retryOn = RetryOn.thrown { it is IOException },
    backoff = ExponentialBackoff(200.milliseconds),
    jitter = FullJitter,
) {
    api.removeUser(user)
}
```

---

## Table of Contents

- [Why RetryKt?](#why-retrykt)
- [Installation](#installation)
- [Compatibility](#compatibility)
- [Quick Start](#quick-start)
- [Retry Policies](#retry-policies)
- [Backoff](#backoff)
- [Jitter](#jitter)
- [Design Goals](#design-goals)
- [Coroutine API](#coroutine-api)
- [Blocking API](#blocking-api)
- [Coroutine Cancellation](#coroutine-cancellation)
- [FAQ](#faq)
- [Supported Platforms](#supported-platforms)
- [License](#license)

---

## Why RetryKt?

`repeat(3)` is fine until retries need real rules. Real-world retry logic often needs to:

- Retry only specific exceptions or returned values
- Use configurable backoff strategies
- Add jitter to avoid synchronized retries
- Respect coroutine cancellation
- Observe retry attempts and outcomes
- Support both suspending and blocking code
- Work consistently across Kotlin Multiplatform

RetryKt provides these capabilities in a small, focused library without framework-specific dependencies.

Instead of writing ad-hoc retry loops, you define **what** to retry (`RetryOn`)
and **how** to schedule retries (`Backoff` + `Jitter`), with optional observability through `RetryListener`.

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

## Compatibility

These are the Kotlin and Coroutines versions used to test this release line.

| RetryKt Version | Kotlin Version | Kotlin Coroutines Version |
|-----------------|----------------|---------------------------|
| 0.2.x           | 2.3.x          | 1.10.x                    |
| 0.3.x           | 2.3.x          | 1.10.x                    |

### JVM Compatibility

The JVM artifact targets Java 11. It is built with JDK 17.

---

## Quick Start

> **Rule of thumb**
>
> Use `retry()` in suspend code.  
> Use `retryBlocking()` everywhere else.

### Retry an operation

```kotlin
val user = retry {
    api.getUser()
}
```

### Use exponential backoff

```kotlin
val user = retry(
    maxAttempts = 5,
    backoff = ExponentialBackoff(
        initialDelay = 100.milliseconds,
        multiplier = 2.0,
        maxDelay = 10.seconds,
    ),
) {
    api.getUser()
}
```

### Add jitter

Jitter is a separate step after backoff, so you can mix and match both.

```kotlin
val response = retry(
    backoff = ExponentialBackoff(
        initialDelay = 200.milliseconds,
        maxDelay = 10.seconds,
    ),
    jitter = FullJitter,
) {
    api.getResponse()
}
```

### Retry only specific exceptions

```kotlin
val user = retry(
    retryOn = RetryOn.thrown { it is IOException },
) {
    api.getUser()
}
```

### Retry returned values

Sometimes an operation succeeds but returns a value that should be retried.

```kotlin
val response = retry(
    retryOn = RetryOn.returned { it.status == 503 },
) {
    api.getResponse()
}
```

### Access the retry context

Each attempt gets a `RetryContext`.

```kotlin
retry(maxAttempts = 3) { ctx ->
    log.info("Attempt ${ctx.attempt}/${ctx.maxAttempts}")

    uploadFile()
}
```

### Observe retry lifecycle

Use `RetryListener` to observe retry attempts and their outcomes.

```kotlin
retry(
    listener = RetryListener(
        onRetry = { event ->
            log.info("Retrying after attempt ${event.context.attempt}.")
        },
        onSuccess = { event ->
            log.info("Succeeded on attempt ${event.context.attempt}.")
        },
        onFailure = { event ->
            log.info("Failed on attempt ${event.context.attempt}.")
        },
    ),
) {
    fetchData()
}
```

---

## Retry Policies

`RetryOn` decides whether the last result deserves another attempt. It can inspect both thrown exceptions and returned
values.

By default, RetryKt retries exceptions and accepts returned values. Kotlin `Error` subclasses pass through. Only retry
an `Error` with an explicit `RetryOn` policy, and only if you really mean it.

### Retry thrown exceptions

For example, retry only network failures:

```kotlin
retry(retryOn = RetryOn.thrown { it is IOException || it is TimeoutException }) {
    request()
}
```

### Retry returned values

Some APIs report temporary failures through return values rather than exceptions.

```kotlin
retry(retryOn = RetryOn.returned { it.status == 503 }) {
    api.getResponse()
}
```

### Retry based on the attempt outcome

Need both the value and the exception? Use `outcome`:

```kotlin
retry(
    retryOn = RetryOn.outcome { outcome ->
        when (outcome) {
            is AttemptOutcome.Returned -> outcome.value.shouldRetry()
            is AttemptOutcome.Thrown -> outcome.throwable is IOException
        }
    },
) {
    request()
}
```

`AttemptOutcome` is the single type used for both cases.

---

## Backoff

A backoff calculates the base delay before the next attempt.

Backoff and jitter are separate concepts:

```text
Backoff
   ↓
raw delay
   ↓
Jitter
   ↓
actual delay (applied delay)
   ↓
wait
```

This separation allows the same backoff strategy to be combined with different jitter strategies.

Built-in backoff implementations include:

```kotlin
NoBackoff            // 0ms
ConstantBackoff      // 100ms, 100ms, 100ms
LinearBackoff        // 100ms, 200ms, 300ms
ExponentialBackoff   // 100ms, 200ms, 400ms
DecorrelatedBackoff  // randomized, based on the previous applied delay
```

Choose the strategy that matches your workload.

| Strategy              | Typical use case                                                        |
|-----------------------|-------------------------------------------------------------------------|
| `NoBackoff`           | Tests, CPU-bound operations                                             |
| `ConstantBackoff`     | Fixed polling intervals                                                 |
| `LinearBackoff`       | Gradually increasing retry intervals                                    |
| `ExponentialBackoff`  | Network requests, cloud APIs, distributed systems                       |
| `DecorrelatedBackoff` | Distributed systems where randomized, decorrelated delays are desirable |

### Exponential backoff

```kotlin
ExponentialBackoff(
    initialDelay = 100.milliseconds,
    multiplier = 2.0,
    maxDelay = 10.seconds,
)
```

### Decorrelated backoff

`DecorrelatedBackoff` is the AWS-style decorrelated-jitter algorithm packaged as a backoff. It uses the actual delay
from the previous retry when calculating the next one.

```kotlin
DecorrelatedBackoff(
    initialDelay = 100.milliseconds,
    maxDelay = 10.seconds,
)
```

It already randomizes delays, so pair it with `NoJitter` unless you deliberately want more randomness.

### Custom backoff

Custom backoffs receive the current attempt and the actual delay used before it.

```kotlin
class MyBackoff : Backoff {

    override fun nextDelay(context: BackoffContext): Duration {
        val attempt = context.attempt
        val lastAppliedDelay = context.lastAppliedDelay

        // ...
    }
}
```

```kotlin
retry(backoff = MyBackoff()) {
    task()
}
```

`lastAppliedDelay` is `null` for the first retry attempt.

---

## Jitter

Jitter changes the delay from backoff. It helps keep many clients from retrying at the same time.

RetryKt applies jitter after backoff:

```text
rawDelay = backoff.nextDelay(...)
appliedDelay = jitter.apply(rawDelay)
```

### Built-in jitter strategies

Built-in jitter strategies:

| Strategy         | Behavior                                                  |
|------------------|-----------------------------------------------------------|
| `NoJitter`       | Leaves the backoff delay unchanged                        |
| `FullJitter`     | Random delay in `[0, rawDelay)`                           |
| `EqualJitter`    | Keeps half of the raw delay and randomizes the other half |
| `AdditiveJitter` | Adds an independent random delay in `[0, maxJitter)`      |

### Full Jitter

Full Jitter picks a random delay between zero and the backoff delay.

```kotlin
retry(
    backoff = ExponentialBackoff(
        initialDelay = 200.milliseconds,
        maxDelay = 10.seconds,
    ),
    jitter = FullJitter,
) {
    request()
}
```

Conceptually:

```text
appliedDelay = random(0, rawDelay)
```

### Equal Jitter

Equal Jitter keeps half the delay, then randomizes the rest.

```text
temp = rawDelay
appliedDelay = temp / 2 + random(0, temp / 2)
```

```kotlin
retry(
    backoff = ExponentialBackoff(200.milliseconds),
    jitter = EqualJitter,
) {
    request()
}
```

### Additive Jitter

`AdditiveJitter` adds an independent random delay.

```kotlin
retry(
    backoff = ExponentialBackoff(200.milliseconds),
    jitter = AdditiveJitter(100.milliseconds),
) {
    request()
}
```

For a raw delay of `200ms`, the resulting delay is in the range:

```text
[200ms, 300ms)
```

Unlike `FullJitter` and `EqualJitter`, its random part does not depend on the backoff delay.

### Custom jitter

`Jitter` is a functional interface, so custom strategies stay small.

```kotlin
class MyJitter : Jitter {

    override fun apply(rawDelay: Duration): Duration {
        // ...
    }
}
```

Or using a lambda:

```kotlin
retry(jitter = { rawDelay ->  /* ... */ }) {
    task()
}
```

---

## Design Goals

RetryKt does retries and leaves the rest to other tools.

### Goals

- Kotlin-first API
- Kotlin Multiplatform support
- Consistent coroutine and blocking APIs
- No framework-specific runtime dependencies
- Explicit retry decisions
- Independent backoff and jitter strategies
- Small, composable building blocks
- Predictable behavior

### Non-goals

It does not try to provide:

- Circuit breakers
- Rate limiting
- Bulkheads
- Service discovery
- Metrics collection
- Scheduling

Use dedicated libraries when you need those pieces.

---

## Coroutine API

Use `retry()` from suspend code.

### Ktor Client

```kotlin
val user = retry(
    retryOn = RetryOn.thrown { it is IOException },
    backoff = ExponentialBackoff(200.milliseconds),
    jitter = FullJitter,
) {
    client.get("/users/$id").body<User>()
}
```

### Repository

```kotlin
class UserRepository(
    private val api: UserApi,
) {

    suspend fun getUser(id: Long): User {
        return retry(backoff = LinearBackoff(200.milliseconds)) {
            api.getUser(id)
        }
    }
}
```

---

## Blocking API

Use `retryBlocking()` when the calling code is synchronous.

On JavaScript and WebAssembly, `retryBlocking()` only supports zero-delay retries. A positive delay throws
`UnsupportedOperationException`: those platforms cannot block the current thread.

### JVM CacheLoader

```kotlin
val cache = Caffeine.newBuilder()
    .build<String, User> { id ->
        retryBlocking {
            api.loadUser(id)
        }
    }
```

### Kotlin/Native C callback

Kotlin/Native callbacks often come from C libraries. Those callbacks cannot be `suspend`, so `retryBlocking()` fits well
here.

Common examples:

- libcurl
- POSIX APIs
- Platform SDKs
- Other native C libraries

```kotlin
// Simplified example

val callback = staticCFunction { chunk ->
    retryBlocking(
        retryOn = RetryOn.thrown { it is IOException },
        backoff = ExponentialBackoff(100.milliseconds),
        jitter = FullJitter,
    ) {
        uploader.send(chunk)
    }
}
```

### Android WorkManager

```kotlin
class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {

    override fun doWork(): Result {
        retryBlocking {
            uploadPendingFiles()
        }

        return Result.success()
    }
}
```

Typical use cases:

| Platform      | Examples                                   |
|---------------|--------------------------------------------|
| JVM           | JDBC, cache loaders, blocking HTTP clients |
| Android       | WorkManager, Binder services               |
| Kotlin/Native | C callbacks, POSIX APIs                    |
| Desktop / CLI | File I/O, external processes               |

---

## Coroutine Cancellation

`CancellationException` is never retried.

When a coroutine is canceled, RetryKt stops right away: it does not call the retry policy or schedule another attempt.

`retryBlocking()` follows the same rule when it sees a `CancellationException`.

---

## FAQ

### Why are there both `retry()` and `retryBlocking()`?

Kotlin has suspend and blocking execution models. RetryKt gives each one its own API but keeps the retry rules the same.

---

### Can I retry successful results?

Yes. Use `RetryOn.returned` or `RetryOn.outcome`.

---

### Can I implement my own backoff strategy?

Yes. Implement `Backoff` and pass it to `retry()` or `retryBlocking()`. `BackoffContext` gives you the attempt number
and the actual delay from the previous retry.

---

### Can I implement my own jitter?

Yes. Implement `Jitter` and pass it independently of backoff.

```kotlin
val jitter = Jitter { rawDelay ->
    rawDelay * Random.nextDouble()
}
```

---

### What is the difference between backoff and jitter?

Backoff chooses the base delay. Jitter changes that delay, usually with randomness.

For example:

```text
ExponentialBackoff
    ↓
100ms → 200ms → 400ms
    ↓
FullJitter
    ↓
random(0, 100)ms → random(0, 200)ms → random(0, 400)ms
```

They are separate so you can combine them freely.

---

### Why does `DecorrelatedBackoff` already contain randomness?

`DecorrelatedBackoff` is the AWS decorrelated-jitter algorithm. Its next delay depends on the actual previous delay and
already includes randomness, so it normally uses `NoJitter`.

---

### Does RetryKt work on Kotlin Multiplatform?

Yes. See [Supported Platforms](#supported-platforms).

---

### Why not use `Flow.retryWhen()`?

`Flow.retryWhen()` is for Flows only. RetryKt works with any suspend or blocking operation and lets you configure
policies, backoff, jitter, and callbacks.

---

## Supported Platforms

RetryKt currently supports:

| Platform                      | Supported |
|-------------------------------|-----------|
| JVM                           | ✅        |
| Android                       | ✅        |
| iOS (x64)                     | ✅        |
| iOS (ARM64)                   | ✅        |
| iOS Simulator (Apple Silicon) | ✅        |
| macOS (Apple Silicon)         | ✅        |
| Windows                       | ✅        |
| Linux x64                     | ✅        |
| Linux ARM64                   | ✅        |
| JavaScript                    | ✅        |
| WebAssembly                   | ✅        |

---

## License

Apache License 2.0.
