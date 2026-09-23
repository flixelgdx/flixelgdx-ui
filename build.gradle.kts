/**
 * Root aggregator for the FlixelGDX UI build.
 *
 * <p>The root project holds no source itself; it only applies IDE plugins and registers the
 * aggregate Javadoc task. All module setup lives in the convention plugins under
 * build-logic/src/main/kotlin/.
 */

plugins {
  eclipse
  idea
  // These plugins must be declared in the root classpath scope to prevent classloader conflicts
  // when convention plugins apply them to the module. Neither is applied to the root project
  // itself; the module applies them via the flixelgdx.* convention plugins.
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.vanniktech) apply false
}

// The root project deliberately has no group. It shares its name with the flixelgdx-ui module, so
// giving it the org.flixelgdx group too would make org.flixelgdx:flixelgdx-ui ambiguous when a
// game includes this build as a composite build. The module gets its group from the convention
// plugins.
version = gitVersion()

eclipse.project.name = "flixelgdx-ui-parent"

fun gitVersion(): String = try {
  val proc = ProcessBuilder("git", "describe", "--tags", "--abbrev=0")
    .directory(rootDir)
    .start()
  proc.waitFor()
  proc.inputStream.bufferedReader().readText().trim().removePrefix("v").ifEmpty { "unspecified" }
} catch (_: Exception) {
  "unspecified"
}

idea {
  module {
    outputDir = file("build/classes/java/main")
    testOutputDir = file("build/classes/java/test")
  }
}

tasks.register("javadocAll") {
  group = "verification"
  description = "Runs Javadoc (with doclint) on the published Java library module."
  dependsOn(":flixelgdx-ui:javadoc")
}
