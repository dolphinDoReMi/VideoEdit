plugins { 
  id("com.android.library")
  kotlin("android") 
}

android {
  namespace = "com.mira.com.feature.clip"
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

dependencies {
  implementation(project(":core:infra"))
  implementation(project(":core:media"))
  implementation("org.json:json:20240303")
  // Swap in a real encoder later:
  // implementation("org.pytorch:pytorch_android_lite:1.13.1")
}
