# Contributing

Use Java 25 and the checked-in Gradle wrapper. Fork the repository and submit a pull request with a concrete description of the behavior being changed.

Run `./gradlew build` before submitting. For GUI or runtime changes, also run `./gradlew runClientGameTest` in a graphical session. Linux can use `xvfb-run -a ./gradlew runClientGameTest`. Tests use a disposable game directory and world.

Keep the frameless Flow design and muted orange palette. Use Fabric APIs first and keep mixins narrowly scoped. Include a regression test when fixing data loss, profile behavior, input, or module effects. Update the changelog and documentation when behavior changes.

Report reproducible bugs through GitHub Issues, including exact versions and other mods. Share only the relevant portion of logs after removing private information.

Maintainers produce artifacts with `./gradlew prepareRelease`. A `v<version>` tag runs verification on Windows, macOS, and Linux, then prepares a draft GitHub Release. Review the artifacts and notes before publishing it. Keep `gradle.properties`, release notes, changelog, and metadata aligned for each release.
