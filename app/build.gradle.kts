plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  kotlin("plugin.serialization")
  kotlin("plugin.compose")
  // Code quality plugins - temporarily disabled
  // id("io.gitlab.arturbosch.detekt")
  // id("org.jlleitschuh.gradle.ktlint")
  // Temporarily disabled for testing progress fixes
  // id("com.google.devtools.ksp")
  // id("dagger.hilt.android.plugin")
  // id("com.google.gms.google-services") // Firebase App Distribution
  // id("com.google.firebase.appdistribution") // Firebase App Distribution plugin
}

android {
  namespace = "com.mira.com"
  compileSdk = 34
  
  lint {
    baseline = file("lint-baseline.xml")
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = "17"
  }
  
  composeOptions {
    kotlinCompilerExtensionVersion = "1.5.8"
  }

  defaultConfig {
    applicationId = "com.mira.com"     // FROZEN across variants
    minSdk = 26
    targetSdk = 34
    versionCode = 1
    versionName = "0.1.0"
    
    // Store metadata (avoid duplicates; defined in strings.xml)
    // resValue("string", "app_description", "AI-powered video editing with automatic clip selection")
    
    // Xiaomi store requirements
    manifestPlaceholders["xiaomi_app_id"] = "mira.ui"
    
    // Make key knots visible at runtime/CI
    buildConfigField("int",    "CLIP_DIM",              "512")
    buildConfigField("int",    "DEFAULT_FRAME_COUNT",   "32")
    buildConfigField("String", "DEFAULT_SCHEDULE",      "\"UNIFORM\"")
    buildConfigField("String", "DEFAULT_DECODE_BACKEND","\"MMR\"")
    buildConfigField("int",    "DEFAULT_MEM_BUDGET_MB", "512")
    buildConfigField("boolean","RETR_USE_L2_NORM",      "true")
    buildConfigField("String", "RETR_SIMILARITY",       "\"cosine\"")
    buildConfigField("String", "RETR_STORAGE_FMT",      "\".f32\"")
    buildConfigField("boolean","RETR_ENABLE_ANN",       "false")

    // Stable, non-appId-derived actions
    buildConfigField("String", "ACTION_CLIP_RUN",       "\"com.mira.clip.CLIP.RUN\"")
    buildConfigField("String", "ACTION_ORCHESTRATE",    "\"com.mira.clip.ORCHESTRATE\"")
    buildConfigField("String", "ACTION_INGEST",         "\"com.mira.clip.INGEST\"")
    buildConfigField("String", "ACTION_SEARCH",         "\"com.mira.clip.SEARCH\"")
    
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
    getByName("debug") {
      // Per-thread install isolation: pass -PappIdSuffix=<suffix>
      val suffixProp = (project.findProperty("appIdSuffix") as String?)?.trim().orEmpty()
      val computedSuffix = if (suffixProp.isNotEmpty()) ".t.$suffixProp" else ""
      applicationIdSuffix = computedSuffix
      isDebuggable = true
      isMinifyEnabled = false
      isShrinkResources = false
      
      buildConfigField("boolean", "DEBUG_MODE", "true")
      buildConfigField("String", "BUILD_TYPE", "\"debug\"")
      buildConfigField("boolean", "ENABLE_LOGGING", "true")

      // Make the suffix visible in the launcher name for easy device triage
      resValue("string", "app_name", "Mira (debug${computedSuffix})")
    }
    
    getByName("release") {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
      signingConfig = signingConfigs.getByName("release")
      
      buildConfigField("boolean", "DEBUG_MODE", "false")
      buildConfigField("String", "BUILD_TYPE", "\"release\"")
      buildConfigField("boolean", "ENABLE_LOGGING", "false")
    }
    
    create("cliptest") {
      initWith(getByName("debug"))
      applicationIdSuffix = ".cliptest"
      versionNameSuffix = "-cliptest"
      
      buildConfigField("boolean", "DEBUG_MODE", "true")
      buildConfigField("String", "BUILD_TYPE", "\"cliptest\"")
      buildConfigField("boolean", "ENABLE_LOGGING", "true")
      buildConfigField("boolean", "CLIP_TEST_MODE", "true")
    }
    
    create("internal") {
      initWith(getByName("release"))
      
      // Internal testing configuration
      versionNameSuffix = "-internal"
      
      buildConfigField("boolean", "DEBUG_MODE", "true")
      buildConfigField("String", "BUILD_TYPE", "\"internal\"")
      buildConfigField("boolean", "ENABLE_LOGGING", "true")
      
      // Keep some debugging info for internal testing
      isMinifyEnabled = false
      isShrinkResources = false
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
//   groups = "internal-testers" // Will be added through Firebase Console
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
  // Feature modules (temporarily disabled to unblock instrumented tests)
  // implementation(project(":feature:clip"))
  
  // Core orchestration dependencies
  implementation("androidx.work:work-runtime-ktx:2.9.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
  implementation("org.json:json:20231013")
  
  // PyTorch Mobile for CLIP models
  implementation("org.pytorch:pytorch_android:1.13.1")
  implementation("org.pytorch:pytorch_android_torchvision:1.13.1")

  // Media3 - versions compatible with API 34
  implementation("androidx.media3:media3-transformer:1.2.1")
  implementation("androidx.media3:media3-effect:1.2.1")
  implementation("androidx.media3:media3-common:1.2.1")
  implementation("androidx.media3:media3-exoplayer:1.2.1") // For preview (optional)

  // UI - Using compatible versions for API 34
  implementation(platform("androidx.compose:compose-bom:2023.08.00"))
  implementation("androidx.activity:activity-compose:1.8.2")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.material:material")
  implementation("androidx.compose.ui:ui-tooling-preview")
  debugImplementation("androidx.compose.ui:ui-tooling")

  // Room database for CLIP4Clip embeddings and video metadata
  implementation("androidx.room:room-runtime:2.7.0")
  // ksp("androidx.room:room-compiler:2.7.0")
  implementation("androidx.room:room-ktx:2.7.0")
  
  // DataStore for settings and preferences
  implementation("androidx.datastore:datastore-preferences:1.1.1")
  
  // Kotlinx Serialization for JSON
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
  
  // Gson for JSON serialization
  implementation("com.google.code.gson:gson:2.11.0")

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
}

// Custom task to verify app configuration
tasks.register("verifyConfig") {
  group = "verification"
  description = "Verifies that the app configuration is valid"
  
  doLast {
    println("Verifying app configuration...")
    
    // Check that required properties are set
    val requiredProperties = listOf(
      "applicationId" to android.defaultConfig.applicationId,
      "minSdk" to android.defaultConfig.minSdk,
      "targetSdk" to android.defaultConfig.targetSdk,
      "versionCode" to android.defaultConfig.versionCode,
      "versionName" to android.defaultConfig.versionName
    )
    
    requiredProperties.forEach { (name, value) ->
      if (value == null) {
        throw GradleException("Required configuration property '$name' is not set")
      }
      println("✓ $name: $value")
    }
    
    // Verify build types exist
    val buildTypes = android.buildTypes.names
    val expectedBuildTypes = listOf("debug", "release", "cliptest", "internal")
    expectedBuildTypes.forEach { buildType ->
      if (buildType in buildTypes) {
        println("✓ Build type '$buildType' exists")
      } else {
        throw GradleException("Required build type '$buildType' is missing")
      }
    }
    
    // Verify signing configuration for release builds
    val releaseSigningConfig = android.buildTypes.getByName("release").signingConfig
    if (releaseSigningConfig != null) {
      println("✓ Release signing configuration is set")
    } else {
      println("⚠ Release signing configuration is not set (will use debug signing)")
    }
    
    println("✓ App configuration verification completed successfully")
  }
}

dependencies {
  // Feature modules (temporarily disabled to unblock instrumented tests)
  // implementation(project(":feature:clip"))
  
  // Core orchestration dependencies
  implementation("androidx.work:work-runtime-ktx:2.9.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
  implementation("org.json:json:20231013")
  
  // PyTorch Mobile for CLIP models
  implementation("org.pytorch:pytorch_android:1.13.1")
  implementation("org.pytorch:pytorch_android_torchvision:1.13.1")

  // Media3 - versions compatible with API 34
  implementation("androidx.media3:media3-transformer:1.2.1")
  implementation("androidx.media3:media3-effect:1.2.1")
  implementation("androidx.media3:media3-common:1.2.1")
  implementation("androidx.media3:media3-exoplayer:1.2.1") // For preview (optional)

  // UI - Using compatible versions for API 34
  implementation(platform("androidx.compose:compose-bom:2023.08.00"))
  implementation("androidx.activity:activity-compose:1.8.2")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.material:material")
  implementation("androidx.compose.ui:ui-tooling-preview")
  debugImplementation("androidx.compose.ui:ui-tooling")

  // Room database for CLIP4Clip embeddings and video metadata
  implementation("androidx.room:room-runtime:2.7.0")
  // ksp("androidx.room:room-compiler:2.7.0")
  implementation("androidx.room:room-ktx:2.7.0")
  
  // DataStore for settings and preferences
  implementation("androidx.datastore:datastore-preferences:1.1.1")
  
  // Kotlinx Serialization for JSON
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
  
  // Gson for JSON serialization
  implementation("com.google.code.gson:gson:2.11.0")

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
