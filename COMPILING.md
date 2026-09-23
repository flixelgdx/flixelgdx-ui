# Compiling & Testing

FlixelGDX UI is a library extension, not a standalone game, so it cannot be run by itself. It also is not fully
self-contained: it depends on the FlixelGDX framework (`org.flixelgdx:flixelgdx-core`). Testing your changes
therefore has two parts: getting the extension to build against the framework, and then consuming your local
extension from a separate test game.

This guide focuses on what is specific to this repository. For the full environment setup (installing JDK 17
with Eclipse Temurin, Git, IDE configuration, and platform troubleshooting), follow the framework's guide,
which applies here unchanged:
**[flixelgdx/flixelgdx -> COMPILING.md](https://github.com/flixelgdx/flixelgdx/blob/master/COMPILING.md)**.

---

## Table of contents

1. [Prerequisites](#prerequisites)
2. [Getting the source](#getting-the-source)
3. [How this extension depends on the framework](#how-this-extension-depends-on-the-framework)
4. [Building the extension](#building-the-extension)
5. [Testing the extension in a game (composite build)](#testing-the-extension-in-a-game-composite-build)
6. [Troubleshooting](#troubleshooting)

---

## Prerequisites

- **Java (JDK 17, Eclipse Temurin).** The build uses the Gradle wrapper and a Java 17 toolchain. Install
  Temurin 17 as described in the framework's
  [COMPILING.md](https://github.com/flixelgdx/flixelgdx/blob/master/COMPILING.md).
- **Git**, to clone this repository and, optionally, the framework.

Verify Java after installing:

```bash
java -version   # should report 17 and mention OpenJDK / Temurin
```

---

## Getting the source

```bash
git clone https://github.com/flixelgdx/flixelgdx-ui.git
cd flixelgdx-ui
```

If you are contributing, fork the repository first, clone your fork, and add the upstream remote, exactly as
described in the framework's
[CONTRIBUTING.md](https://github.com/flixelgdx/flixelgdx/blob/master/CONTRIBUTING.md).

---

## How this extension depends on the framework

The `flixelgdx-ui` module declares the framework as an ordinary external dependency, pinned to the `flixelgdx`
version in [`gradle/libs.versions.toml`](gradle/libs.versions.toml):

```
flixelgdx-ui -> org.flixelgdx:flixelgdx-core
```

Gradle resolves that coordinate from the repositories declared in
[`settings.gradle.kts`](settings.gradle.kts):

- **Maven Central** - the normal case once a framework release is published.
- **Sonatype OSS** (release and snapshot repositories) - for framework builds published ahead of a Maven
  Central sync.
- **JitPack** - a framework build from a GitHub branch or commit, referenced by its JitPack coordinates.

### Building against a local framework checkout automatically

Unlike some other FlixelGDX extensions, this repository does not require any extra flag to build against a
sibling clone. [`settings.gradle.kts`](settings.gradle.kts) checks whether `../flixelgdx` exists next to this
repository and, if it does, includes it as a composite build automatically:

```kotlin
if (file("../flixelgdx").isDirectory) {
  includeBuild("../flixelgdx")
}
```

This substitutes the framework artifacts (`org.flixelgdx:flixelgdx-core`, and `flixelgdx-jvm` for tests) with
your local `../flixelgdx` checkout by module coordinates, so the `flixelgdx` version number in the catalog does
not need to match while the sibling checkout is present. Any framework change is picked up on the next build
with no republishing. Clone the framework next to this repository to opt in:

```bash
cd ..
git clone https://github.com/flixelgdx/flixelgdx.git
cd flixelgdx-ui
```

Remove or rename the `../flixelgdx` directory to go back to resolving the framework from a repository.

---

## Building the extension

Build the module:

```bash
./gradlew assemble
```

Run the same checks CI runs on every push:

```bash
./gradlew spotlessCheck    # formatting
./gradlew checkstyleMain   # code quality and Javadoc completeness
./gradlew javadocAll       # Javadoc with doclint on the published module
./gradlew test             # unit tests
```

Apply formatting fixes automatically before committing:

```bash
./gradlew spotlessApply
```

---

## Testing the extension in a game (composite build)

To actually see a widget on screen, you consume your local extension from a test game. There are two ways to
point it at your local extension.

### Method 1: Composite build (recommended)

A **Gradle composite build** makes the test game compile directly against your extension source, so every
change is picked up on the next build with no republishing.

1. Open your test game project.
2. In the game's **`settings.gradle`** (or `settings.gradle.kts`), include the extension build below the
   `rootProject.name` line:
   ```gradle
   includeBuild '/path/to/flixelgdx-ui'
   ```
   - **Windows**: use forward slashes, e.g. `C:/Users/You/flixelgdx-ui`.
   - **macOS/Linux**: e.g. `/home/you/projects/flixelgdx-ui`.
   - If you are also changing the framework, also include it: `includeBuild '/path/to/flixelgdx'`. If your
     `flixelgdx-ui` checkout has a sibling `../flixelgdx` directory, its own composite build already picks that
     up (see above), so you usually only need to add the one `includeBuild` line here.
3. Declare the dependency normally in the matching module; the composite build substitutes it with your local
   project automatically:
   ```gradle
   implementation 'org.flixelgdx:flixelgdx-ui:<version>'
   ```
4. Build your UI as described in the [README](README.md), and add the display to a state.
5. Refresh Gradle and run the game.

> [!NOTE]
> The version string in the dependency does not have to match while a composite build is active; Gradle
> substitutes by module coordinates (group and name), so your local extension is used regardless of the number
> you write.

### Method 2: `mavenLocal()`

If you prefer not to use a composite build:

1. Publish the extension to your local Maven repository:
   ```bash
   ./gradlew publishToMavenLocal
   ```
2. In the test game's root `build.gradle`, add `mavenLocal()` to `repositories` before `mavenCentral()`.

Re-run `publishToMavenLocal` each time you change the extension and want the game to pick it up. Use Method 1
to avoid that.

---

## Troubleshooting

### `Could not find org.flixelgdx:flixelgdx-core` (or `-jvm`)

- **Cause**: the framework version this extension targets (`flixelgdx` in
  [`gradle/libs.versions.toml`](gradle/libs.versions.toml)) is not available in any of the repositories
  configured in [`settings.gradle.kts`](settings.gradle.kts), and no sibling `../flixelgdx` checkout is present.
- **Fix**: clone the framework next to this repository (`../flixelgdx`), as described in
  [Building against a local framework checkout automatically](#building-against-a-local-framework-checkout-automatically),
  so the framework builds from source instead of being resolved from a repository.

### Spotless failures

- **Fix**: run `./gradlew spotlessApply` and commit the reformatted files.

### Checkstyle failures

- **Fix**: read the reported rule name and location in the console output, or the HTML report under
  `flixelgdx-ui/build/reports/checkstyle/`. Most failures are missing Javadoc, a banned standard Java
  collection, or a fully-qualified `@link`; see [AGENTS.md](AGENTS.md) for the full rule set.

For anything not covered here, the framework's
[COMPILING.md](https://github.com/flixelgdx/flixelgdx/blob/master/COMPILING.md#troubleshooting) troubleshooting
section applies to this repository as well.
