plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.digitalwallet"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.digitalwallet"
        minSdk = 26
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

        // Firebase
        implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
        implementation("com.google.firebase:firebase-auth")
        implementation("com.google.firebase:firebase-database")

        // Charting
        implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

        // UI Utilities
        implementation("de.hdodenhof:circleimageview:3.1.0")
        implementation("com.airbnb.android:lottie:6.7.1")

}