plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("app.cash.sqldelight")
}

group = "org.distributed.calendar"

version = "1.0"

repositories { mavenCentral() }

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":core-common"))
    implementation("app.cash.sqldelight:coroutines-extensions-jvm:2.0.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}

sqldelight {
    databases {
        register("CalendarDatabase") {
            packageName.set("org.distributed.calendar.db")
        }
    }
}
