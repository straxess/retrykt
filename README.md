# RetryKt

[![Maven Central](https://img.shields.io/maven-central/v/io.github.straxess/retrykt)](https://central.sonatype.com/artifact/io.github.straxess/retrykt)
[![Build](https://github.com/straxess/retrykt/actions/workflows/gradle.yml/badge.svg)](https://github.com/straxess/retrykt/actions/workflows/gradle.yml)
[![codecov](https://codecov.io/gh/straxess/retrykt/graph/badge.svg)](https://codecov.io/gh/straxess/retrykt)
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![License](https://img.shields.io/github/license/straxess/retrykt)](LICENSE)

> A lightweight Kotlin Multiplatform retry library with coroutine and blocking APIs.

RetryKt provides a small, composable retry API for KMP. It supports configurable retry policies, backoff strategies,
jitter, lifecycle listeners, and both suspending and blocking execution.

RetryKt focuses on reliable retry behavior without trying to become a general-purpose resilience framework.

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
- [Observing Retries](#observing-retries)
- [Coroutine API](#coroutine-api)
- [Blocking API](#blocking-api)
- [Coroutine Cancellation](#coroutine-cancellation)
- [Design Goals](#design-goals)
- [FAQ](#faq)
- [Supported Platforms](#supported-platforms)
- [License](#license)

---

## Why RetryKt?

A simple `repeat(3)` loop is enough for trivial cases. Real-world retry logic often needs more:

- Retry only specific exceptions or returned values
- Configure how delays grow between attempts
- Add jitter to avoid synchronized retries
- Respect coroutine cancellation
- Observe retry attempts and outcomes
- Support both suspending and blocking operations
- Run consistently across Kotlin Multiplatform targets

RetryKt separates these concerns into small, composable building blocks:

- **`RetryOn`** — decides whether an outcome should be retried
- **`Backoff`** — calculates the base delay
- **`Jitter`** — modifies the backoff delay
- **`RetryListener`** — observes the retry lifecycle
- **`RetryContext`** — provides information about the current attempt

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

RetryKt is built and tested with the following Kotlin and Coroutines versions:

| RetryKt | Kotlin | Kotlin Coroutines |
|---------|--------|-------------------|
| 0.4.x   | 2.3.x  | 1.10.x            |

### JVM

The JVM artifact targets Java 11. It is built with JDK 17.

---

## Quick Start

### Retry an operation

Use `retry()` for suspending operations:

```kotlin
val user = retry {
    api.getUser()
}
```

By default, thrown exceptions are retried and returned values are accepted.

### Limit the number of attempts

```kotlin
val user = retry(maxAttempts = 5) {
    api.getUser()
}
```

### Retry specific exceptions

```kotlin
val user = retry(
    retryOn = RetryOn.thrown { it is IOException },
) {
    api.getUser()
}
```

### Retry returned values

An operation does not have to throw an exception to be retried:

```kotlin
val response = retry(
    retryOn = RetryOn.returned { it.status == 503 },
) {
    api.getResponse()
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

Backoff and jitter are independent and can be combined:

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

### Access the retry context

Each attempt receives a `RetryContext`:

```kotlin
retry(maxAttempts = 3) { ctx ->
    log.info("Attempt ${ctx.attempt}/${ctx.maxAttempts}")

    if (ctx.prevOutcome is AttemptOutcome.Thrown) {
        log.info("Previous attempt failed.")
    }

    uploadFile()
}
```

`RetryContext` describes the current attempt and the outcome of the previous attempt.

---

## Retry Policies

`RetryOn` determines whether the outcome of an attempt should be retried.

It can inspect:

- thrown exceptions
- returned values
- the complete `AttemptOutcome`

### Retry thrown exceptions

Retry only selected exceptions:

```kotlin
retry(
    retryOn = RetryOn.thrown {
        it is IOException || it is TimeoutException
    },
) {
    request()
}
```

### Retry returned values

Some APIs report temporary failures through return values:

```kotlin
retry(
    retryOn = RetryOn.returned { it.status == 503 },
) {
    api.getResponse()
}
```

### Retry based on the outcome

Use `RetryOn.outcome` when the policy needs to handle both returned values and exceptions:

```kotlin
retry(
    retryOn = RetryOn.outcome { outcome ->
        when (outcome) {
            is AttemptOutcome.Returned -> outcome.value == null
            is AttemptOutcome.Thrown -> outcome.throwable is IOException
        }
    },
) {
    request()
}
```

`AttemptOutcome` is the single type used for both cases.

### Errors

Kotlin `Error` subclasses are not retried by the default policy.

If you explicitly configure a `RetryOn` policy that matches an `Error`, it can be retried.

---

## Backoff

A backoff calculates the **base delay** before the next attempt.

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

Built-in backoff implementations include the following examples:

| Backoff              | Attempt 1 | Attempt 2 | Attempt 3 | Attempt 4 | Attempt 5 | Attempt 6 |
|----------------------|----------:|----------:|----------:|----------:|----------:|----------:|
| `NoBackoff`          |        0s |        0s |        0s |        0s |        0s |        0s |
| `ConstantBackoff`    |        1s |        1s |        1s |        1s |        1s |        1s |
| `LinearBackoff`      |        1s |        2s |        3s |        4s |        5s |        6s |
| `FibonacciBackoff`   |        1s |        1s |        2s |        3s |        5s |        8s |
| `ExponentialBackoff` |        1s |        2s |        4s |        8s |       16s |       32s |

`DecorrelatedBackoff` is randomized and therefore is not represented by a fixed sequence.

Choose the strategy that matches your workload:

| Strategy              | Typical use case                                                        |
|-----------------------|-------------------------------------------------------------------------|
| `NoBackoff`           | Tests and immediate retries                                             |
| `ConstantBackoff`     | Fixed polling intervals                                                 |
| `LinearBackoff`       | Gradually increasing retry intervals                                    |
| `FibonacciBackoff`    | Moderate growth between linear and exponential                          |
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

`DecorrelatedBackoff` implements the AWS-style decorrelated-jitter algorithm. It uses the applied delay from the
previous retry when calculating the next delay.

```kotlin
DecorrelatedBackoff(
    initialDelay = 100.milliseconds,
    maxDelay = 10.seconds,
)
```

Because it already introduces randomness, it normally does not need an additional jitter strategy:

```kotlin
retry(
    backoff = DecorrelatedBackoff(100.milliseconds),
    jitter = NoJitter,
) {
    request()
}
```

### Custom backoff

Implement `Backoff` to provide your own strategy:

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

Jitter modifies the delay produced by the backoff strategy.

It is useful when many clients may otherwise retry at the same time:

```text
rawDelay = backoff.nextDelay(...)
appliedDelay = jitter.apply(rawDelay)
```

Built-in jitter strategies:

| Strategy         | Behavior                                                  |
|------------------|-----------------------------------------------------------|
| `NoJitter`       | Leaves the backoff delay unchanged                        |
| `FullJitter`     | Random delay in `[0, rawDelay)`                           |
| `EqualJitter`    | Keeps half of the raw delay and randomizes the other half |
| `AdditiveJitter` | Adds an independent random delay in `[0, maxJitter)`      |

### Full Jitter

Full Jitter selects a random delay between zero and the raw backoff delay:

```text
appliedDelay = random(0, rawDelay)
```

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

### Equal Jitter

Equal Jitter keeps half of the raw delay and randomizes the remaining half:

```text
appliedDelay = rawDelay / 2 + random(0, rawDelay / 2)
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

`AdditiveJitter` adds an independent random delay:

```kotlin
retry(
    backoff = ExponentialBackoff(200.milliseconds),
    jitter = AdditiveJitter(100.milliseconds),
) {
    request()
}
```

For a raw delay of `200ms`, the resulting delay is in `[200ms, 300ms)`

Unlike `FullJitter` and `EqualJitter`, the random component is independent of the backoff delay.

### Custom jitter

`Jitter` is a functional interface, so custom strategies can remain small:

```kotlin
class MyJitter : Jitter {

    override fun apply(rawDelay: Duration): Duration {
        // ...
    }
}
```

Or use a lambda:

```kotlin
retry(jitter = { rawDelay ->  /* ... */ }) {
    task()
}
```

---

## Observing Retries

Use `RetryListener` to observe the retry lifecycle without changing retry behavior.

```kotlin
retry(
    listener = RetryListener(
        onRetry = { event, decision ->
            log.info("Retrying after attempt ${event.context.attempt}, waiting ${decision.nextDelay}.")
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

`RetryEvent` describes a completed attempt:

- `outcome` — whether the attempt returned a value or threw an exception
- `context` — the `RetryContext` for that attempt

`RetryDecision` describes the decision for the next retry attempt.

Currently, it exposes the delay that will be applied: `decision.nextDelay`

The `onRetry` callback receives both objects.

`onRetry` is called after the retry decision has been made and before the delay is applied.

---

## Coroutine API

Use `retry()` from `suspend` code.

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

Use `retryBlocking()` when the calling code is synchronous:

```kotlin
val user = retryBlocking {
    api.loadUser()
}
```

On JavaScript and WebAssembly, `retryBlocking()` supports only zero-delay retries. A positive delay throws
`UnsupportedOperationException` because these platforms cannot block the current thread.

### JVM CacheLoader

```kotlin
val cache = Caffeine.newBuilder()
    .build<String, User> { id ->
        retryBlocking {
            api.loadUser(id)
        }
    }
```

### Kotlin/Native C callbacks

Kotlin/Native callbacks from C APIs cannot be `suspend`, making `retryBlocking()` useful for synchronous native
integration.

Typical examples include:

- libcurl
- POSIX APIs
- platform SDKs
- other native C libraries

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

Typical blocking use cases:

| Platform      | Examples                                   |
|---------------|--------------------------------------------|
| JVM           | JDBC, cache loaders, blocking HTTP clients |
| Android       | WorkManager, Binder services               |
| Kotlin/Native | C callbacks, POSIX APIs                    |
| Desktop / CLI | File I/O, external processes               |

---

## Coroutine Cancellation

`CancellationException` is never retried.

When a coroutine is canceled, RetryKt stops without invoking the retry policy or scheduling another attempt.

`retryBlocking()` follows the same rule when it encounters a `CancellationException`.

---

## Design Goals

RetryKt focuses on retry behavior and leaves other resilience concerns to dedicated tools.

### Goals

- Kotlin-first API
- Kotlin Multiplatform support
- Consistent coroutine and blocking APIs
- No framework-specific runtime dependencies
- Explicit retry policies
- Independent backoff and jitter strategies
- Small, composable building blocks
- Predictable behavior

### Non-goals

RetryKt does not provide:

- Circuit breakers
- Rate limiting
- Bulkheads
- Metrics collection
- General-purpose scheduling

Use dedicated libraries when you need these capabilities.

---

## FAQ

### Why are there both `retry()` and `retryBlocking()`?

Kotlin has suspending and blocking execution models. RetryKt provides a dedicated API for each while keeping retry
policies, backoff, jitter, and lifecycle behavior consistent.

### Can I retry successful results?

Yes. Use `RetryOn.returned` or `RetryOn.outcome`.

### Can I retry specific exceptions?

Yes. Use `RetryOn.thrown`:

```kotlin
retry(
    retryOn = RetryOn.thrown { it is IOException },
) {
    request()
}
```

### Can I implement my own backoff strategy?

Yes. Implement `Backoff` and pass it to `retry()` or `retryBlocking()`.

`BackoffContext` provides the attempt number and the applied delay used before the current retry.

### Can I implement my own jitter?

Yes. Implement `Jitter` and use it independently of the backoff strategy:

```kotlin
val jitter = Jitter { rawDelay ->
    rawDelay * Random.nextDouble()
}
```

### What is the difference between backoff and jitter?

Backoff chooses the base delay. Jitter modifies that delay, typically by introducing randomness.

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

They are separate so you can combine different backoff and jitter strategies.

### Why does `DecorrelatedBackoff` already contain randomness?

`DecorrelatedBackoff` implements the AWS-style decorrelated-jitter algorithm. Its next delay depends on the applied
previous delay and includes randomness, so it normally does not need an additional jitter strategy.

### Does RetryKt work with Kotlin Multiplatform?

Yes. RetryKt supports JVM, Android, Kotlin/Native, JavaScript, and WebAssembly targets.

See [Supported Platforms](#supported-platforms).

---

## Supported Platforms

RetryKt currently supports:

| Platform                          | Supported |
|-----------------------------------|-----------|
| JVM                               | ✅        |
| Android                           | ✅        |
| Android Native (x64)              | ✅        |
| Android Native (ARM64)            | ✅        |
| iOS (x64)                         | ✅        |
| iOS (ARM64)                       | ✅        |
| iOS Simulator (Apple Silicon)     | ✅        |
| watchOS (ARM64)                   | ✅        |
| watchOS Simulator (Apple Silicon) | ✅        |
| tvOS (ARM64)                      | ✅        |
| tvOS Simulator (Apple Silicon)    | ✅        |
| macOS (Apple Silicon)             | ✅        |
| Windows (x64)                     | ✅        |
| Linux (x64)                       | ✅        |
| Linux (ARM64)                     | ✅        |
| JavaScript                        | ✅        |
| WebAssembly                       | ✅        |

---

## License

Apache License 2.0.
