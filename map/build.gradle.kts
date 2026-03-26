plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.even.map"
    compileSdk = 34
    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    flavorDimensions.add("environment")
    productFlavors {
        create("dev") { dimension = "environment" }
        create("staging") { dimension = "environment" }
        create("production") { dimension = "environment" }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
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
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("androidx.compose.ui:ui:1.4.0")
    implementation("com.mapbox.mapboxsdk:mapbox-sdk-turf:5.3.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("org.locationtech.proj4j:proj4j:1.3.0")
    implementation("org.locationtech.proj4j:proj4j-epsg:1.3.0")
    implementation("com.google.guava:guava:32.0.1-android")
    implementation("com.github.haifengl:smile-kotlin:3.1.1") {
        exclude(group = "org.bytedeco", module = "javacpp")
        exclude(group = "org.bytedeco", module = "openblas")
    }

    implementation(project(":core"))
    implementation(project(":core:logger"))
}
