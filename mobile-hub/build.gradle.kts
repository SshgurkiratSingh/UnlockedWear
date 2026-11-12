plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.unlockedwear.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.unlockedwear.mobile"
        minSdk = 28
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
    // Jetpack Compose Material 3 (Phone UI)
    // =========================================================================
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)

    // =========================================================================
    // Wearable Data Layer API (Google Play Services)
    // =========================================================================
    implementation(libs.play.services.wearable)

    // =========================================================================
    // Dependency Injection (Hilt & Hilt Work)
    // =========================================================================
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)

    // =========================================================================
    // Local Database & Preferences (Room & DataStore)
    // =========================================================================
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)

    // =========================================================================
    // Background Tasks (WorkManager)
    // =========================================================================
    implementation(libs.work.runtime.ktx)

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
    implementation(libs.kotlinx.serialization.json)

    // =========================================================================
    // Testing & Debug Tooling
    // =========================================================================
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
