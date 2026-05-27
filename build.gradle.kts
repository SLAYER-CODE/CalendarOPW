plugins {
    // Kotlin version aligned for Compose Multiplatform
    id("org.jetbrains.kotlin.jvm") version "1.9.23" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23" apply false
    // no declarar plugin 'org.jetbrains.kotlin.plugin.compose' aquí; los módulos que usen Compose
    // deben aplicar el plugin 'org.jetbrains.compose' o el plugin Android Compose según corresponda

    id("com.android.application") version "8.6.1" apply false
    id("app.cash.sqldelight") version "2.0.2" apply false
}
