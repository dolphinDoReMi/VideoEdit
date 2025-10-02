plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  // Temporarily disabled for testing progress fixes
  // id("com.google.devtools.ksp")
  // id("dagger.hilt.android.plugin")
  // id("com.google.gms.google-services") // Firebase App Distribution
  // id("com.google.firebase.appdistribution") // Firebase App Distribution plugin
}

android {
  namespace = "com.mira.videoeditor"
  compileSdk = 34

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = "17"
  }

  defaultConfig {
    applicationId = "com.mira.videoeditor"
    minSdk = 24
    targetSdk = 34
    versionCode = 1
    versionName = "0.1.0"
    
    // Store metadata
    resValue("string", "app_name", "Mira")
    resValue("string", "app_description", "AI-powered video editing with automatic clip selection")
    
    // Xiaomi store requirements
    manifestPlaceholders["xiaomi_app_id"] = "com.mira.videoeditor"
    
    // NDK configuration removed - no longer using native code
  }

  signingConfigs {
    create("release") {
      // Store signing configuration from gradle.properties
      val keystoreFile = project.findProperty("KEYSTORE_FILE") as String?
      val keystorePassword = project.findProperty("KEYSTORE_PASSWORD") as String?
      val keyAlias = project.findProperty("KEY_ALIAS") as String?
      val keyPassword = project.findProperty("KEY_PASSWORD") as String?
      
      if (keystoreFile != null && keystorePassword != null && keyAlias != null && keyPassword != null) {
        storeFile = file(keystoreFile)
        storePassword = keystorePassword
        this.keyAlias = keyAlias
        this.keyPassword = keyPassword
      } else {
        // Fallback for development - will fail for release builds
        storeFile = file("../keystore/mira-release.keystore")
        storePassword = "dev_password"
        this.keyAlias = "mira"
        this.keyPassword = "dev_password"
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
      signingConfig = signingConfigs.getByName("release")
      
      // Release optimizations
      buildConfigField("boolean", "DEBUG_MODE", "false")
      buildConfigField("String", "BUILD_TYPE", "\"release\"")
      buildConfigField("boolean", "ENABLE_LOGGING", "false")
      
      // Store release configuration
      applicationIdSuffix = ""
      versionNameSuffix = ""
    }
    
    create("internal") {
      initWith(getByName("release"))
      
      // Internal testing configuration
      applicationIdSuffix = ".internal"
      versionNameSuffix = "-internal"
      
      buildConfigField("boolean", "DEBUG_MODE", "true")
      buildConfigField("String", "BUILD_TYPE", "\"internal\"")
      buildConfigField("boolean", "ENABLE_LOGGING", "true")
      
      // Keep some debugging info for internal testing
      isMinifyEnabled = false
      isShrinkResources = false
    }
    
    debug {
      // Keep readable logs for testing
      isMinifyEnabled = false
      isShrinkResources = false
      isDebuggable = true  // Explicitly enable debugging for profiler
      
      buildConfigField("boolean", "DEBUG_MODE", "true")
      buildConfigField("String", "BUILD_TYPE", "\"debug\"")
      buildConfigField("boolean", "ENABLE_LOGGING", "true")
      
      // Debug configuration
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-debug"
    }
  }

  buildFeatures { 
    compose = true
    buildConfig = true
    prefab = true
  }
  
  // Native library configuration removed - no longer using native code
  
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  
  kotlinOptions {
    jvmTarget = "17"
  }
  packaging { 
    resources.pickFirsts += listOf("META-INF/*")
    // Optimize APK size
    jniLibs {
      useLegacyPackaging = false
    }
  }
  
  // Xiaomi store specific configurations
  bundle {
    language {
      enableSplit = true
    }
    density {
      enableSplit = true
    }
    abi {
      enableSplit = true
    }
  }

  testOptions {
    unitTests.isIncludeAndroidResources = true
    animationsDisabled = true
  }
}

// Firebase App Distribution configuration (temporarily disabled)
// firebaseAppDistribution {
//   appId = "1:384262830567:android:1960eb5e2470beb09ce542" // Firebase App ID
//   // groups = "internal-testers" // Will be added through Firebase Console
//   releaseNotes = """
//     Mira v0.1.0-internal
//     
//     Features:
//     - AI-powered video editing
//     - Automatic clip selection
//     - Motion-based scoring
//     - Simple one-tap editing
//     
//     Testing Focus:
//     - Core functionality
//     - Performance on different devices
//     - UI/UX feedback
//     - Bug reporting
//   """.trimIndent()
// }

dependencies {
  // Media3 - versions compatible with API 34
  implementation("androidx.media3:media3-transformer:1.2.1")
  implementation("androidx.media3:media3-effect:1.2.1")
  implementation("androidx.media3:media3-common:1.2.1")
  implementation("androidx.media3:media3-exoplayer:1.2.1") // For preview (optional)

  // UI - Using compatible versions for API 34
  implementation(platform("androidx.compose:compose-bom:2023.10.01"))
  implementation("androidx.activity:activity-compose:1.8.2")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.material:material")
  implementation("androidx.compose.ui:ui-tooling-preview")
  debugImplementation("androidx.compose.ui:ui-tooling")

  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

  // Room database for CLIP4Clip embeddings and video metadata
  // implementation("androidx.room:room-runtime:2.7.0")
  // ksp("androidx.room:room-compiler:2.7.0")
  // implementation("androidx.room:room-ktx:2.7.0")
  
  // DataStore for settings and preferences
  implementation("androidx.datastore:datastore-preferences:1.1.1")
  
  // WorkManager for background video ingestion
  implementation("androidx.work:work-runtime-ktx:2.9.1")
  
  // PyTorch Mobile for CLIP models (temporarily disabled for WhisperEngine testing)
  // implementation("org.pytorch:pytorch_android_lite:2.3.0")
  // implementation("org.pytorch:pytorch_android_torchvision:2.3.0")

  // HTTP client for optional cloud upload
  implementation("com.squareup.okhttp3:okhttp:4.12.0")

  // Firebase App Distribution (handled by plugin)

  // (Optional) ML Kit face detection: boost score weight for "people scenes"
  // Use "unbundled version" (smaller size, requires model download on first use)
  implementation("com.google.mlkit:face-detection:16.1.7")
  
  // Dependency Injection with Hilt
  // implementation("com.google.dagger:hilt-android:2.48")
  // ksp("com.google.dagger:hilt-compiler:2.48")
  // implementation("androidx.hilt:hilt-work:1.1.0")
  
  // Security - SQLCipher for encrypted database
  implementation("net.zetetic:android-database-sqlcipher:4.5.4")
  
  // Performance monitoring
  implementation("androidx.tracing:tracing:1.2.0")

  // Test dependencies
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.robolectric:robolectric:4.12.2")
  testImplementation("androidx.room:room-testing:2.7.0")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

  androidTestImplementation("androidx.test:runner:1.6.2")
  androidTestImplementation("androidx.test.ext:junit:1.2.1")
  androidTestImplementation("androidx.work:work-testing:2.9.1")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
