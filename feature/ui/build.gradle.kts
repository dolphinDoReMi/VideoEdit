plugins { 
  id("com.android.library")
  kotlin("android")
  id("org.jetbrains.kotlin.plugin.compose")
}

android { 
  namespace = "com.mira.com.feature.ui"
  compileSdk = 34
  
  defaultConfig { 
    minSdk = 26 
  }
  
  buildFeatures { 
    compose = true 
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
  implementation("androidx.compose.ui:ui:1.6.8")
  implementation("androidx.compose.material3:material3:1.2.1")
}
