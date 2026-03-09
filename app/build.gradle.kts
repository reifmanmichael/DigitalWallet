plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.digitalwallet"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.digitalwallet"
        minSdk = 30
        targetSdk = 36
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("androidx.core:core-ktx:1.17.0")

    // BOM
    implementation(platform(libs.firebase.bom))


// Firebase (ללא גרסאות)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.ai)


// One-shot (Guava ListenableFuture)
    implementation(libs.guava)


// Streaming (Reactive Streams Publisher)
    implementation(libs.reactive.streams)
    // Charting
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // UI Utilities
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.airbnb.android:lottie:6.7.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0") // For Real Currency API
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("androidx.core:core-splashscreen:1.0.1")

    // Gemini AI
    implementation(libs.generativeai)
    implementation(libs.guava)
}