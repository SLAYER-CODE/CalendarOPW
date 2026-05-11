plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {

    namespace = "org.distributed.calendar.android"

    compileSdk = 35

    defaultConfig {

        applicationId = "org.distributed.calendar.android"

        minSdk = 26

        targetSdk = 35

        versionCode = 1

        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {

    implementation(project(":core"))

    implementation("androidx.core:core-ktx:1.13.1")

    implementation("androidx.activity:activity-compose:1.9.0")

    implementation("androidx.compose.ui:ui:1.6.8")

    implementation("androidx.compose.material3:material3:1.2.1")

    implementation("com.google.android.material:material:1.12.0")
}
