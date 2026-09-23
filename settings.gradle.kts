/**
 * Root settings for the FlixelGDX UI build.
 *
 * <p>Declares the build-logic included build so convention plugins are available to the module,
 * and centralizes repository declarations, including where the published FlixelGDX framework
 * artifact (`org.flixelgdx:flixelgdx-core`) is resolved from.
 */

pluginManagement {
  includeBuild("build-logic")
  repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://s01.oss.sonatype.org")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
  repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://s01.oss.sonatype.org")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io")
  }
}

rootProject.name = "flixelgdx-ui"

include("flixelgdx-ui")

// When a sibling framework checkout is present, build against it directly instead of a published
// artifact. This substitutes `org.flixelgdx:flixelgdx-core` (and any other framework module the
// composite build resolves, such as `flixelgdx-jvm` for tests) with the local `../flixelgdx`
// projects by module coordinates, so framework changes are picked up without republishing.
if (file("../flixelgdx").isDirectory) {
  includeBuild("../flixelgdx")
}
