# Contributing to RetryKt

Thank you for contributing to RetryKt. Keep changes focused, preserve the small explicit API, and update tests and docs
whenever observable behavior changes.

## Development environment

- Use JDK 17.
- Use the Gradle wrapper.
- IntelliJ IDEA is the expected IDE.

## Final verification

Run this command before submitting a change:

```shell
./gradlew build
```

This is the project's canonical verification command. The Gradle build lifecycle checks code style through
`ktlintCheck`, compiles the configured Kotlin Multiplatform targets, and runs every test supported by the current host.
Platform-specific tasks may be skipped when their binaries cannot execute on that host.

## Kotlin formatting

RetryKt intentionally configures ktlint with the `intellij_idea` profile. The profile matches IntelliJ IDEA's default
Kotlin formatting behavior, preventing the IDE formatter and ktlint from rewriting the same code differently.

Do not switch ktlint to another profile or apply project-wide formatting with different IDE settings without an explicit
project decision. Format changed Kotlin files with IntelliJ IDEA's default Kotlin formatter and let the build verify the
result.

## Change expectations

- Keep shared behavior in `commonMain` unless a platform-specific implementation is required.
- Preserve suspend and blocking API parity except for documented platform limitations.
- Add focused tests for behavior changes and boundary conditions.
- Update public KDoc and README.md examples when API contracts or observable behavior change.
- Avoid unrelated public API, publishing, signing, or dependency changes.
