plugins {
    kotlin("multiplatform")
    // Apply Compose plugin (version declared in settings.pluginManagement)
    id("org.jetbrains.compose")
    // Android Gradle Plugin is required for kotlin.androidTarget()
    id("com.android.library")
}

android {
    // Minimal Android library configuration required by the Android Gradle Plugin
    namespace = "org.distributed.calendar.ui"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    jvmToolchain(21)
    jvm()
    androidTarget()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material3)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(compose.ui)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(compose.ui)
            }
        }
    }
}
