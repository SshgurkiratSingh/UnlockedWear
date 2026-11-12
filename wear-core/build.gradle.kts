plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.unlockedwear.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.unlockedwear.wear"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

ksp {
    arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
}


dependencies {
    // Shared Protocol module
    implementation(project(":shared-protocol"))

    // =========================================================================
    // 1. Samsung Health Sensor SDK (Local AAR)
    // =========================================================================
    implementation(files("libs/samsung-health-sensor-api.aar"))

    // =========================================================================
    // 2. Wear OS Complications
    // =========================================================================
    implementation(libs.wear.watchface.complications.data.source)
    implementation(libs.wear.watchface.complications.data.source.ktx)
    implementation(libs.wear.watchface.complications.data)

    // =========================================================================
    // 3. Wear OS Tiles & ProtoLayout
    // =========================================================================
    implementation(libs.wear.tiles)
    implementation(libs.wear.tiles.material)
    implementation(libs.wear.tiles.tooling)
    implementation(libs.wear.tiles.tooling.preview)
    implementation(libs.wear.protolayout)
    implementation(libs.wear.protolayout.material)
    implementation(libs.wear.protolayout.expression)

    // =========================================================================
    // 4. Wearable Data Layer API (Google Play Services)
    // =========================================================================
    implementation(libs.play.services.wearable)

    // =========================================================================
    // 5. Wear Compose (Full Suite)
    // =========================================================================
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)
    implementation(libs.wear.compose.ui.tooling)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // =========================================================================
    // Dependency Injection (Hilt)
    // =========================================================================
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // =========================================================================
    // Local Database & Preferences (Room & DataStore)
    // =========================================================================
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)

    // =========================================================================
    // Networking (Ktor Client)
    // =========================================================================
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    // =========================================================================
    // Google AI Client SDK (Gemini)
    // =========================================================================
    implementation(libs.google.ai.generativeai)

    // =========================================================================
    // Core Android & Coroutines
    // =========================================================================
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.10.1")
    implementation(libs.kotlinx.serialization.json)


    // =========================================================================
    // Testing
    // =========================================================================
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
