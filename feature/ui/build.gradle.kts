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
}

dependencies {
  implementation(project(":core:infra"))
  implementation("androidx.compose.ui:ui:1.6.8")
  implementation("androidx.compose.material3:material3:1.2.1")
}
