# RetryKt

[![Maven Central](https://img.shields.io/maven-central/v/io.github.straxess/retrykt)](https://central.sonatype.com/artifact/io.github.straxess/retrykt)
[![Build](https://github.com/straxess/retrykt/actions/workflows/gradle.yml/badge.svg)](https://github.com/straxess/retrykt/actions/workflows/gradle.yml)
[![codecov](https://codecov.io/gh/straxess/retrykt/graph/badge.svg)](https://codecov.io/gh/straxess/retrykt)
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![License](https://img.shields.io/github/license/straxess/retrykt)](LICENSE)

RetryKt is a small Kotlin Multiplatform library for repeating operations when their results are rejected. It works with
suspending and blocking code and lets you control:

- which results should be retried;
- how long to wait between attempts;
- whether to add a random delay;
- how to observe retries, accepted results, and exhausted retries.

```kotlin
val user = retry {
    api.getUser()
}
```

```kotlin
val user = retry(
    maxAttempts = 5,
    retryOn = RetryOn.thrown { it is IOException },
    backoff = ExponentialBackoff(firstDelay = 200.milliseconds, maxDelay = 10.seconds),
    jitter = FullJitter,
) {
    api.getUser()
}
```

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

## Compatibility

| RetryKt | Kotlin | Kotlin Coroutines |
|---------|--------|-------------------|
| 0.5.x   | 2.3.x  | 1.10.x            |
| 0.4.x   | 2.3.x  | 1.10.x            |

The JVM artifact targets Java 11 and is built with JDK 17. Android requires API level 24 or newer.

## Basic use

Use `retry()` in suspending code:

```kotlin
val user = retry {
    api.getUser()
}
```

By default, RetryKt retries thrown exceptions, except Kotlin `Error` subclasses. It accepts all returned values.

The default `maxAttempts` is `Int.MAX_VALUE`. Set a smaller value when an operation can keep failing:

```kotlin
val user = retry(maxAttempts = 5) {
    api.getUser()
}
```

`maxAttempts` includes the first call and must be greater than zero.

## Choosing what to retry

`RetryOn` decides whether another attempt is needed.

Retry selected exceptions:

```kotlin
retry(
    retryOn = RetryOn.thrown { it is IOException || it is TimeoutException },
) {
    request()
}
```

Retry selected returned values:

```kotlin
retry(
    retryOn = RetryOn.returned { response -> response.status == 503 },
) {
    api.getResponse()
}
```

Use `RetryOn.outcome` to check both kinds of result:

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

`AttemptOutcome.Returned` contains a returned value. `AttemptOutcome.Thrown` contains an exception.

If a thrown exception does not match the policy, RetryKt passes it to the caller without wrapping it. An exception
thrown by the policy itself is also passed through unchanged.

## Waiting between attempts

A `Backoff` calculates a delay after a rejected outcome. A `Jitter` can then randomize that delay before the retry:

```text
Backoff -> backoff delay -> Jitter -> next applied delay -> delay()/sleep() -> next attempt
```

### Backoff strategies

| Strategy              | Delays when configured with 1 second  |
|-----------------------|---------------------------------------|
| `NoBackoff`           | `0, 0, 0, 0, ...`                     |
| `ConstantBackoff`     | `1, 1, 1, 1, ...`                     |
| `LinearBackoff`       | `1, 2, 3, 4, ...`                     |
| `FibonacciBackoff`    | `1, 1, 2, 3, 5, ...`                  |
| `ExponentialBackoff`  | `1, 2, 4, 8, 16, ...`                 |
| `DecorrelatedBackoff` | Random; depends on the previous delay |

Growing strategies require a finite `maxDelay`:

```kotlin
val backoff = ExponentialBackoff(
    firstDelay = 100.milliseconds,
    multiplier = 2.0,
    maxDelay = 10.seconds,
)
```

The first attempt starts immediately. `firstDelay` is the delay before the first retry, which is attempt 2.
`maxDelay` limits the delay produced by the backoff strategy. Jitter is applied afterward, so a strategy such as
`AdditiveJitter` can make `nextAppliedDelay` greater than `maxDelay`.

`DecorrelatedBackoff` already adds randomness, so it is normally used without extra jitter:

```kotlin
retry(
    backoff = DecorrelatedBackoff(firstDelay = 100.milliseconds, maxDelay = 10.seconds),
    jitter = NoJitter,
) {
    request()
}
```

Delay settings must follow these rules:

- every delay must be finite and non-negative;
- `firstDelay` or `increment` must not be greater than `maxDelay`;
- `ExponentialBackoff.multiplier` must be finite and at least `1.0`.

A zero `firstDelay` or `increment` produces zero delays for deterministic growing strategies. For
`DecorrelatedBackoff`, later delays can become positive if jitter first makes the applied delay positive.

### Jitter strategies

Jitter helps prevent many clients from retrying at the same time.

| Strategy         | Next applied delay                               |
|------------------|--------------------------------------------------|
| `NoJitter`       | The backoff delay                                |
| `FullJitter`     | Random value from `0` to the backoff delay       |
| `EqualJitter`    | Half the backoff delay plus a random second half |
| `AdditiveJitter` | Backoff delay plus `0..maxJitter`                |

Example:

```kotlin
retry(
    backoff = ExponentialBackoff(firstDelay = 200.milliseconds, maxDelay = 10.seconds),
    jitter = FullJitter,
) {
    request()
}
```

Duration rounding means a random range can include its upper bound. For `AdditiveJitter`, `backoff delay + maxJitter`
must stay finite.

### Custom strategies

Implement `Backoff` or `Jitter` to define a strategy:

```kotlin
class MyBackoff : Backoff {
    override fun calculateDelay(context: BackoffContext): Duration {
        return context.attempt.seconds
    }
}

val myJitter = Jitter { backoffDelay ->
    backoffDelay * Random.nextDouble()
}
```

`BackoffContext.attempt` is the number of the completed attempt. `prevAppliedDelay` is the applied delay passed to the
waiting function before that attempt, or `null` after the first attempt. It is not the measured waiting time, which can
be longer depending on the platform and operating system.

A custom strategy must return a finite, non-negative duration. RetryKt throws `IllegalStateException` if it does not.
Exceptions thrown inside a strategy are passed to the caller unchanged.

## Reading the attempt context

The task receives a `RetryContext`:

```kotlin
retry(maxAttempts = 3) { context ->
    log.info("Attempt ${context.attempt}/${context.maxAttempts}")

    if (context.prevOutcome is AttemptOutcome.Thrown) {
        log.info("The previous attempt threw an exception")
    }

    uploadFile()
}
```

`attempt` starts at 1. `prevOutcome` is `null` on the first attempt.

## Observing retries

Use `RetryListener` for logging, tracing, or metrics:

```kotlin
retry(
    listener = RetryListener(
        onRetry = { event, plan ->
            log.info("Attempt ${event.context.attempt} was rejected; retrying in ${plan.nextAppliedDelay}")
        },
        onSuccess = { event ->
            log.info("Succeeded on attempt ${event.context.attempt}")
        },
        onFailure = { event ->
            log.info("Retrying stopped after attempt ${event.context.attempt}")
        },
    ),
) {
    fetchData()
}
```

- `onRetry` runs after an attempt is marked for retry and before the delay.
- `onSuccess` runs when a returned value is accepted.
- `onFailure` runs when retrying ends without a successful result: either an exception is not retryable or the attempt
  limit is reached while the last outcome is still rejected.

Callbacks run synchronously. An exception from a callback stops retrying and is passed to the caller. Cancellation and
invalid strategy delays do not call a final listener callback.

## `retry()` or `retryBlocking()`?

Choose the function based on the code that calls it:

| Function          | Use it from                          | How it waits between attempts        |
|-------------------|--------------------------------------|--------------------------------------|
| `retry()`         | A coroutine or `suspend` function    | Suspends without blocking the thread |
| `retryBlocking()` | Synchronous code that cannot suspend | Blocks the current thread            |

For example, Ktor's HTTP client has a suspending API, so it should use `retry()`:

```kotlin
suspend fun loadUser(client: HttpClient, id: Long): User =
    retry(
        maxAttempts = 5,
        backoff = ExponentialBackoff(firstDelay = 100.milliseconds, maxDelay = 5.seconds),
    ) {
        client.get("https://api.example.com/users/$id").body<User>()
    }
```

Some APIs require a normal, non-suspending callback. A JVM cache loader is one example:

```kotlin
val cache = Caffeine.newBuilder()
    .build<String, User> { id ->
        retryBlocking(maxAttempts = 3) {
            api.loadUser(id)
        }
    }
```

Kotlin/Native callbacks passed to C also cannot be suspending. Use `retryBlocking()` when such a callback must wait
between attempts:

```kotlin
// Simplified example
val uploadCallback = staticCFunction { chunk ->
    retryBlocking(maxAttempts = 5) {
        uploader.send(chunk)
    }
}
```

`retryBlocking()` exists for cases like blocking JVM APIs, Android `Worker.doWork()`, and synchronous Kotlin/Native
callbacks. Do not use it when the caller can suspend, because blocking keeps the thread occupied during every retry
delay.

Both functions use the same retry policies, backoff, jitter, context, and listeners.

JavaScript and WebAssembly cannot block the current thread. On these platforms, `retryBlocking()` works only when the
delay is zero; a positive delay throws `UnsupportedOperationException`.

## Cancellation and exhausted retries

`retry()` respects coroutine cancellation, including cancellation during a delay. `CancellationException` is never sent
to `RetryOn` or retried. `retryBlocking()` also passes this exception through unchanged.

If the last allowed result is still retryable, RetryKt throws `RetryExhaustedException`:

- `reason` is `RetryExhaustionReason.MaxAttemptsReached(maxAttempts)`;
- `lastOutcome` contains the value or exception from the last attempt;
- `cause` is the last exception when `lastOutcome` is `AttemptOutcome.Thrown`.

## Supported platforms

| Platform                          | Supported |
|-----------------------------------|-----------|
| JVM                               | ✅        |
| Android                           | ✅        |
| Android Native (x64, ARM64)       | ✅        |
| iOS (x64, ARM64, simulator ARM64) | ✅        |
| watchOS (ARM64, simulator ARM64)  | ✅        |
| tvOS (ARM64, simulator ARM64)     | ✅        |
| macOS (Apple Silicon)             | ✅        |
| Windows (x64)                     | ✅        |
| Linux (x64, ARM64)                | ✅        |
| JavaScript                        | ✅        |
| WebAssembly                       | ✅        |

JavaScript and WebAssembly tests run on Node.js; CI does not currently test browsers. CI runs tests for targets that
work on its host. For other Native targets, it compiles or links the test binaries.

## Development

See [CONTRIBUTING.md](CONTRIBUTING.md) for setup and contribution checks.

## License

Apache License 2.0.
