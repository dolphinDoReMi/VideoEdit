plugins { 
  id("com.android.library")
  kotlin("android") 
}

android { 
  namespace = "com.mira.com.feature.retrieval"
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
  
  testImplementation("org.jetbrains.kotlin:kotlin-test:2.1.0")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.1.0")
}
