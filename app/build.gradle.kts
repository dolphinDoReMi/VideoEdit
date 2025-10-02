plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("com.google.devtools.ksp")
  // Temporarily disabled for testing progress fixes
  // id("dagger.hilt.android.plugin")
  // id("com.google.gms.google-services") // Firebase App Distribution
  // id("com.google.firebase.appdistribution") // Firebase App Distribution plugin
}

android {
  namespace = "com.mira.clip"
  compileSdk = 34

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = "17"
  }
  
  composeOptions {
    kotlinCompilerExtensionVersion = "1.5.3"
  }

  defaultConfig {
    applicationId = "com.mira.clip"
    minSdk = 26
    targetSdk = 34
    versionCode = 1
    versionName = "0.1.0"
    
    // Store metadata
    resValue("string", "app_name", "mira_clip")
    resValue("string", "app_description", "AI-powered video editing with automatic clip selection")
    
    // Xiaomi store requirements
    manifestPlaceholders["xiaomi_app_id"] = "com.mira.clip"
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
  implementation("androidx.room:room-runtime:2.6.1")
  implementation("androidx.room:room-ktx:2.6.1")
  ksp("androidx.room:room-compiler:2.6.1")

  implementation("androidx.work:work-runtime-ktx:2.9.1")
  androidTestImplementation("androidx.work:work-testing:2.9.1")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")

  // AndroidX AppCompat
  implementation("androidx.appcompat:appcompat:1.6.1")
  implementation("androidx.core:core-ktx:1.12.0")

  // DataStore for settings
  implementation("androidx.datastore:datastore-preferences:1.0.0")

  // PyTorch dependencies
  implementation("org.pytorch:pytorch_android_lite:1.13.1")

  // Hilt dependencies (temporarily disabled for build issues)
  // implementation("com.google.dagger:hilt-android:2.48")
  // ksp("com.google.dagger:hilt-compiler:2.48")

  testImplementation("junit:junit:4.13.2")
  testImplementation("org.robolectric:robolectric:4.12.2")
  testImplementation("androidx.room:room-testing:2.6.1")
  androidTestImplementation("androidx.test:runner:1.6.2")
  androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
