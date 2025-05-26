plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
    id("com.google.firebase.crashlytics") version "3.0.1"
    id("com.google.firebase.firebase-perf") version "1.4.2" apply false
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Core Android dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.core:core-ktx:1.12.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation("com.google.firebase:firebase-firestore:24.4.3")
    implementation("com.google.firebase:firebase-storage:20.3.0")
    implementation("com.google.firebase:firebase-messaging:23.4.1")

    // Image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Maps and location
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // UI Components
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    implementation("com.google.android.material:material:1.11.0")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // CameraX for modern camera implementation
    implementation("androidx.camera:camera-core:1.3.2")
    implementation("androidx.camera:camera-camera2:1.3.2")
    implementation("androidx.camera:camera-lifecycle:1.3.2")
    implementation("androidx.camera:camera-view:1.3.2")

    // Permissions handling
    implementation("com.karumi:dexter:6.2.3")

    // Image compression
    implementation("id.zelory:compressor:3.0.1")
}