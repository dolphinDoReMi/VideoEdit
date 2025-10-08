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
    
    externalNativeBuild {
      cmake {
        cppFlags("-std=c++17")
        arguments(
          "-DANDROID_STL=c++_shared",
          "-DGGML_USE_CPU=1",
          "-DGGML_USE_OPENMP=1",
          "-DGGML_USE_ACCELERATE=1",
          "-DGGML_USE_METAL=0",
          "-DGGML_USE_CUDA=0",
          "-DGGML_USE_VULKAN=0",
          "-DGGML_USE_SYCL=0",
          "-DGGML_USE_KOMPUTE=0",
          "-DGGML_USE_RPC=0",
          "-DGGML_USE_FLASH_ATTN=0",
          "-DGGML_USE_F16=0",
          "-DGGML_F16_VEC=0",
          "-DGGML_F16_SCALAR=0"
          // Vulkan flags disabled for CPU-only test
          // "-DGGML_VULKAN=1",
          // "-DGGML_VULKAN_DEBUG=1",
          // "-DGGML_VULKAN_CHECK_RESULTS=1"
        )
      }
    }
    
    ndk {
      abiFilters += listOf("arm64-v8a")  // Focus on ARM64 for Xiaomi Pad
    }
  }
  
  externalNativeBuild {
    cmake {
      path = file("src/main/cpp/CMakeLists.txt")
      version = "3.22.1"
    }
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
  implementation(project(":core:media"))
  implementation(project(":core:ml"))
  implementation("org.json:json:20240303")
  
  // WorkManager
  implementation("androidx.work:work-runtime-ktx:2.9.0")
  
  // DocumentFile for SAF
  implementation("androidx.documentfile:documentfile:1.0.1")
  
  // Core for NotificationCompat
  implementation("androidx.core:core-ktx:1.12.0")
  
  // Room
  implementation("androidx.room:room-runtime:2.7.0")
  implementation("androidx.room:room-ktx:2.7.0")
  kapt("androidx.room:room-compiler:2.7.0")
  
  // Test dependencies
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.robolectric:robolectric:4.12.2")
  testImplementation("androidx.room:room-testing:2.7.0")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
  testImplementation("androidx.test.ext:junit:1.2.1")
  testImplementation("androidx.test:core:1.5.0")
  testImplementation("androidx.test:runner:1.5.2")
  testImplementation("androidx.test:rules:1.5.0")
  testImplementation("androidx.arch.core:core-testing:2.2.0")

  androidTestImplementation("androidx.test:runner:1.5.2")
  androidTestImplementation("androidx.test.ext:junit:1.2.1")
  androidTestImplementation("androidx.work:work-testing:2.9.1")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
  androidTestImplementation("androidx.test:core:1.5.0")
  androidTestImplementation("androidx.test:rules:1.5.0")
}
