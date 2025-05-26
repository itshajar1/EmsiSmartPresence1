plugins {
    alias(libs.plugins.android.application)

    id("com.google.gms.google-services")
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
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


}


dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.firebase.auth)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation ("com.google.firebase:firebase-firestore:24.4.3")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")  // Fixed spelling
    implementation ("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation ("androidx.gridlayout:gridlayout:1.0.0")
    implementation ("com.google.android.material:material:1.9.0" ) // Updated to latest stable version

    // Add these if you're using CardView or other components
    implementation ("androidx.cardview:cardview:1.0.0")
    // OkHttp pour les requêtes HTTP
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("com.prolificinteractive:material-calendarview:1.4.0")

// Gson pour la manipulation JSON
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("org.json:json:20230227")
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