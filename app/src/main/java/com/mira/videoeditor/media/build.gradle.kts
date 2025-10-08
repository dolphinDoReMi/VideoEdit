plugins { 
  id("com.android.library")
  kotlin("android") 
}

android { 
  namespace = "com.mira.com.core.media"
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
  
  buildTypes {
    getByName("debug") {
      // Debug configuration
    }
    
    getByName("release") {
      // Release configuration
    }
    
    create("internal") {
      // Internal testing configuration
      initWith(getByName("release"))
      isMinifyEnabled = false
      isShrinkResources = false
    }
  }
}

dependencies {
  implementation(project(":core:infra"))
  implementation("androidx.media3:media3-extractor:1.4.1")
  implementation("androidx.media3:media3-common:1.4.1")
}
