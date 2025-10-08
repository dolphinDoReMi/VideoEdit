plugins { 
  id("com.android.library")
  kotlin("android") 
}

android {
  namespace = "com.mira.com.core.ml"
  compileSdk = 34
  
  defaultConfig { 
    minSdk = 26
    externalNativeBuild { 
      cmake { 
        cppFlags.add("-std=c++17") 
      } 
    }
  }
  
  externalNativeBuild { 
    cmake { 
      path = file("src/main/cpp/CMakeLists.txt") 
    } 
  }
  
  ndkVersion = "26.3.11579264"
  
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
}
