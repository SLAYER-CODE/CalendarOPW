pluginManagement {
  repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    gradlePluginPortal()
  }
  plugins {
    // Centralize Compose Multiplatform plugin version for modules
    // Use a published 1.6.x release that exists in Maven Central
    // 1.6.10 avoids the Compose resources publication requirement for KGP 2.0
    id("org.jetbrains.compose") version "1.6.10"
  }
}

dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "distributed-calendar"

include(":core", ":androidApp")
include(":core-common")
include(":linuxApp")
include(":ui-common")
