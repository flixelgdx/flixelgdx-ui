plugins {
  id("flixelgdx.java-library")
}

dependencies {
  api(libs.flixelgdx.core)
  implementation(libs.jetbrains.annotations)

  testImplementation(libs.flixelgdx.jvm)
  testRuntimeOnly(libs.junit.platform.launcher)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}
