plugins { 
  id("com.android.library")
  kotlin("android")
  id("kotlin-kapt")
}

android { 
  namespace = "com.mira.com.feature.whisper"
  compileSdk = 34
  
  defaultConfig { 
    minSdk = 26 
  }
  
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
  implementation(project(":core:ml"))
  implementation("org.json:json:20240303")
  
  // WorkManager
  implementation("androidx.work:work-runtime-ktx:2.9.0")
  
  // Room
  implementation("androidx.room:room-runtime:2.7.0")
  implementation("androidx.room:room-ktx:2.7.0")
  kapt("androidx.room:room-compiler:2.7.0")
}
