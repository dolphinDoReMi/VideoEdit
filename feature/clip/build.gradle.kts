plugins {
  id("com.android.library")
  kotlin("android")
}

android {
  namespace = "com.mira.com.feature.clip"
  compileSdk = 34

  defaultConfig {
    minSdk = 26
    targetSdk = 34
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    getByName("release") { isMinifyEnabled = false }
    getByName("debug")   { /* inherits */ }
    create("internal")   { isMinifyEnabled = false }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions { jvmTarget = "11" }
}

dependencies {
  implementation("org.json:json:20231013")
  implementation(project(":core:infra"))
  implementation(project(":core:media"))
  implementation(project(":feature:retrieval"))
}
