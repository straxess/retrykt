# RetryKt

[![Maven Central](https://img.shields.io/maven-central/v/io.github.straxess/retrykt)](https://central.sonatype.com/artifact/io.github.straxess/retrykt)
[![Build](https://github.com/straxess/retrykt/actions/workflows/gradle.yml/badge.svg)](https://github.com/straxess/retrykt/actions/workflows/gradle.yml)
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![License](https://img.shields.io/github/license/straxess/retrykt)](LICENSE)

> A lightweight Kotlin Multiplatform retry library with coroutine and blocking APIs.

RetryKt provides a consistent retry model across Kotlin platforms with explicit retry policies, configurable backoff
strategies, jitter, and a minimal runtime footprint.

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

Retrying an operation is often more complex than calling `repeat(3)`.

Production applications typically need:

- Retry only specific failures
- Retry exceptions or returned values
- Configurable backoff strategies
- Jitter to avoid synchronized retries
- Coroutine cancellation awareness
- A consistent API across Kotlin Multiplatform

RetryKt provides these capabilities in a small, focused library without framework-specific dependencies.

Instead of writing ad-hoc retry loops, you define **what** should be retried (`RetryOn`) and **how** retries are
scheduled (`Backoff` + `Jitter`).

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

RetryKt is tested against the following Kotlin and Kotlin Coroutines versions.

| RetryKt Version | Kotlin Version | Kotlin Coroutines Version |
|-----------------|----------------|---------------------------|
| 0.1.x           | 2.3.x          | 1.10.x                    |

### JVM Compatibility

RetryKt JVM artifacts target Java 11 bytecode.

The library is built with JDK 17 and supports Java 11+ runtimes.

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

Jitter is configured independently of the backoff strategy.

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

Every attempt receives a `RetryContext`.

```kotlin
retry(maxAttempts = 3) { ctx ->
    log.info("Attempt ${ctx.attempt}/${ctx.maxAttempts}")

    uploadFile()
}
```

### Observe retry attempts

The `onRetryAttempt` callback is invoked after the retry delay has been calculated and immediately before the wait.

```kotlin
retry(
    onRetryAttempt = { event ->
        log.info("Attempt ${event.context.attempt} failed. Retrying in ${event.plan.nextDelay}.")
    },
) {
    fetchData()
}
```

---

## Retry Policies

`RetryOn` defines whether another retry attempt should be performed based on the outcome of the previous attempt.

Unlike many retry libraries that only inspect exceptions, RetryKt can also retry returned values.

By default, thrown exceptions are retried and returned values complete immediately.

### Retry thrown exceptions

Retry only network-related failures.

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

For advanced scenarios, inspect both successful and failed attempts.

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

`AttemptOutcome` provides a unified model for retry decisions regardless of whether the operation completed normally or
failed with an exception.

---

## Backoff

A backoff strategy calculates the base delay before the next attempt.

Backoff and jitter are separate concepts:

```text
Backoff
   ↓
raw delay
   ↓
Jitter
   ↓
applied delay
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

`DecorrelatedBackoff` implements the AWS-style decorrelated jitter algorithm directly as a backoff strategy.

It uses the actual delay applied before the previous retry attempt when calculating the next delay.

```kotlin
DecorrelatedBackoff(
    initialDelay = 100.milliseconds,
    maxDelay = 10.seconds,
)
```

Because `DecorrelatedBackoff` already incorporates randomization, it is normally used with `NoJitter`.

### Custom backoff

Custom strategies can inspect the current attempt and the delay that was actually applied before the previous retry.

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

Jitter randomizes the delay produced by a backoff strategy.

Without jitter, multiple clients following the same deterministic backoff schedule may retry at approximately the same
time, creating synchronized retry bursts.

RetryKt applies jitter independently after the backoff strategy calculates its raw delay:

```text
rawDelay = backoff.nextDelay(...)
appliedDelay = jitter.apply(rawDelay)
```

### Built-in jitter strategies

RetryKt provides several common jitter algorithms.

| Strategy         | Behavior                                                  |
|------------------|-----------------------------------------------------------|
| `NoJitter`       | Leaves the backoff delay unchanged                        |
| `FullJitter`     | Random delay in `[0, rawDelay)`                           |
| `EqualJitter`    | Keeps half of the raw delay and randomizes the other half |
| `AdditiveJitter` | Adds an independent random delay in `[0, maxJitter)`      |

### Full Jitter

Full Jitter chooses a random delay between zero and the calculated backoff delay.

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

Equal Jitter always retains half of the calculated delay and randomizes the remaining half.

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

`AdditiveJitter` adds a uniformly distributed random delay independently of the backoff delay.

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

Unlike `FullJitter` and `EqualJitter`, the amount of randomization does not depend on the calculated backoff delay.

### Custom jitter

`Jitter` is a functional interface, so custom strategies can be defined concisely.

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

RetryKt intentionally focuses on one problem: reliable retries.

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

RetryKt intentionally does **not** implement higher-level resilience patterns such as:

- Circuit breakers
- Rate limiting
- Bulkheads
- Service discovery
- Metrics collection
- Scheduling

These concerns are better handled by dedicated libraries.

---

## Coroutine API

Use `retry()` in coroutine-based code.

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

Use `retryBlocking()` whenever the execution context is synchronous and a `suspend` function cannot be called.

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

Kotlin/Native frequently integrates with C libraries through callbacks. Since callback signatures are defined by the
native library, they cannot be `suspend`, making `retryBlocking()` a natural fit.

Typical examples include integrations with:

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

Whenever a coroutine is canceled, RetryKt immediately propagates the exception without evaluating retry policies or
scheduling another attempt.

This ensures correct structured concurrency behavior and avoids delaying coroutine cancellation.

The same rule applies to `retryBlocking()`: `CancellationException` is propagated immediately rather than being treated
as a retryable failure.

---

## FAQ

### Why are there both `retry()` and `retryBlocking()`?

Kotlin separates suspend and blocking execution models.

RetryKt provides dedicated APIs for both while keeping the retry model identical.

---

### Can I retry successful results?

Yes.

Retry decisions are not limited to exceptions.

Use `RetryOn.returned` or `RetryOn.outcome` to retry based on returned values.

---

### Can I implement my own backoff strategy?

Yes.

Implement the `Backoff` interface and pass your implementation to `retry()` or `retryBlocking()`.

Your implementation receives a `BackoffContext` containing the current attempt and the actual delay applied before the
previous retry.

---

### Can I implement my own jitter?

Yes.

Implement `Jitter` and pass it independently of the backoff strategy.

```kotlin
val jitter = Jitter { rawDelay ->
    rawDelay * Random.nextDouble()
}
```

---

### What is the difference between backoff and jitter?

Backoff determines the base delay according to the retry schedule.

Jitter modifies that delay, typically by introducing randomization.

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

This separation allows different backoff and jitter strategies to be composed independently.

---

### Why does `DecorrelatedBackoff` already contain randomness?

`DecorrelatedBackoff` is based on the AWS decorrelated jitter algorithm.

Unlike deterministic backoff strategies, its next delay depends on the actual delay used before the previous retry and
includes randomization as part of the algorithm itself.

It is therefore normally used with `NoJitter`.

---

### Does RetryKt work on Kotlin Multiplatform?

Yes.

See [Supported Platforms](#supported-platforms).

---

### Why not use `Flow.retryWhen()`?

`Flow.retryWhen()` only applies to Kotlin Flows.

RetryKt works with any suspend or blocking operation and provides configurable retry policies, backoff strategies,
jitter, and retry callbacks.

---

## Supported Platforms

RetryKt is built with Kotlin Multiplatform and supports the following targets:

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
