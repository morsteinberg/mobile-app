plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("plugin.compose")
}

android {
    namespace = "com.even.map.terraexplorer"
    compileSdk = 34

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    kotlin {
        compilerOptions {
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }

    flavorDimensions.add("environment")
    productFlavors {
        create("dev") { dimension = "environment" }
        create("staging") { dimension = "environment" }
        create("production") { dimension = "environment" }
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    lint {
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    // Location Services
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Coroutine
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Compose
    implementation("androidx.compose.ui:ui:1.4.0")

    // TerraExplorer
    implementation("com.skyline:terraexplorer:1.0.30")

    // Kotlin Reflect
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.21")

    // MapController Interface
    implementation(project(":core"))
    implementation(project(":core:logger"))
    implementation(project(":map"))
}
