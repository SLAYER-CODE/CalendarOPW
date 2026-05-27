plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    application
}

group = "org.distributed.calendar"
version = "1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":core-common"))
    implementation(project(":core"))
    implementation(project(":ui-common"))

    // Ktor server (Netty engine for JVM)
    implementation("io.ktor:ktor-server-core:2.3.12")
    implementation("io.ktor:ktor-server-websockets:2.3.12")
    implementation("io.ktor:ktor-server-netty:2.3.12")

    // Ktor client (CIO engine for JVM)
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation("io.ktor:ktor-client-websockets:2.3.12")

    // SQLDelight JDBC driver for persistence
    implementation("app.cash.sqldelight:sqlite-driver:2.0.2")

    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.foundation)
    implementation(compose.ui)
}

application {
    mainClass.set("LinuxMainKt")
}
