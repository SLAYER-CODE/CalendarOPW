plugins {
    kotlin("jvm")
    // Align serialization plugin with root Kotlin version 1.9.23
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
}

group = "org.distributed.calendar"
version = "1.0"

repositories { mavenCentral() }

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
