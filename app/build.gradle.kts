plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
  // Temporarily disabled for testing progress fixes
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
  }
  
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
  implementation(platform("androidx.compose:compose-bom:2024.10.00"))
  implementation("androidx.activity:activity-compose:1.8.2")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.material:material")
  implementation("androidx.compose.ui:ui-tooling-preview")
  debugImplementation("androidx.compose.ui:ui-tooling")

  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

  // Firebase App Distribution (handled by plugin)

  // (Optional) ML Kit face detection: boost score weight for "people scenes"
  // Use "unbundled version" (smaller size, requires model download on first use)
  implementation("com.google.mlkit:face-detection:16.1.7")
}
