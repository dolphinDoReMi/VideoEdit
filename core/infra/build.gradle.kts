plugins {
    id("com.android.library")
    kotlin("android")
}
android {
    namespace = "com.mira.com.core.infra"
    compileSdk = 34
    defaultConfig { minSdk = 26 }
    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
dependencies { implementation("androidx.core:core-ktx:1.13.1") }
