# Contributing to RetryKt

Keep each change focused. If behavior changes, add tests and update the related KDoc and README example.

## Setup

- Install JDK 17.
- Use the included Gradle wrapper.
- Use IntelliJ IDEA's default Kotlin formatter.

The project uses ktlint's `intellij_idea` profile, which matches that formatter. Do not change the profile or reformat
the whole project without prior agreement.

## Code changes

- Put shared behavior in `commonMain`. Add platform-specific code only when needed.
- Keep the suspending and blocking APIs consistent, except for documented platform limits.
- Test changed behavior and important boundary cases.
- Do not include unrelated API, dependency, publishing, or signing changes.

## Before submitting

Run:

```shell
./gradlew build
```

This command checks formatting, compiles the supported Kotlin Multiplatform targets, and runs tests available on your
host. Some platform-specific tests may be skipped when the host cannot run their binaries.
