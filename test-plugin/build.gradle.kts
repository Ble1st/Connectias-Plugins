plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.ble1st.connectias.testplugin"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ble1st.connectias.testplugin"
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
        // Three-Process UI Architecture: No Compose needed in plugin!
        // UI is rendered in separate UI Process
        compose = false
        buildConfig = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Connectias Plugin SDK (Three-Process UI Architecture support)
    // IMPORTANT: compileOnly to avoid bundling SDK classes into the plugin APK.
    // Bundling the SDK would create duplicate classes across ClassLoaders and break runtime casts.
    compileOnly(project(":connectias-plugin-sdk"))

    // Kotlin standard library
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.3.0")

    // AndroidX Core (minimal)
    implementation("androidx.core:core-ktx:1.17.0")

    // Timber Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Coroutines for async operations
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
}
