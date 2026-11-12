plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.unlockedwear.watchface"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.unlockedwear.watchface"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
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
}

dependencies {
    // Watch Face Format complications & metadata support
    implementation(libs.wear.watchface.complications.data)
}
