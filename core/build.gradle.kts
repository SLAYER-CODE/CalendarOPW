plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("app.cash.sqldelight")
    id("application")
}

group = "org.distributed.calendar"

version = "1.0"

repositories { mavenCentral() }

dependencies {
  implementation(kotlin("stdlib"))
implementation(
    "app.cash.sqldelight:sqlite-driver:2.0.2"

)
implementation("app.cash.sqldelight:coroutines-extensions-jvm:2.0.2")
  implementation("io.ktor:ktor-server-core-jvm:2.3.12")
  implementation("io.ktor:ktor-server-websockets-jvm:2.3.12")
  implementation("io.ktor:ktor-server-netty-jvm:2.3.12")

  implementation("io.ktor:ktor-client-core-jvm:2.3.12")
  implementation("io.ktor:ktor-client-cio-jvm:2.3.12")
  implementation("io.ktor:ktor-client-websockets-jvm:2.3.12")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
}

// application { mainClass.set("MainKt") }

tasks.register<JavaExec>("runServer") {
  group = "application"
  mainClass.set("ServerMainKt")

  classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("runClient") {
  group = "application"
  mainClass.set("ClientMainKt")

  classpath = sourceSets["main"].runtimeClasspath
}

sqldelight {

    databases {

        register("CalendarDatabase") {

            packageName.set(
                "org.distributed.calendar.db"
            )
        }
    }
}
