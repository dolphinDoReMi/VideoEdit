plugins { 
  id("com.android.library")
  kotlin("android") 
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
}
