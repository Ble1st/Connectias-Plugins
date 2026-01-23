plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.ble1st.connectias.pingplugin"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ble1st.connectias.pingplugin"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    buildFeatures {
        buildConfig = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Plugin SDK
    implementation(project(":plugin-sdk-temp:connectias-plugin-sdk"))
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.3.0")
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.17.0")
    
    // Timber Logging
    implementation("com.jakewharton.timber:timber:5.0.1")
}
