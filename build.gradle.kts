plugins {
  id("com.android.application") version "8.1.4" apply false // Stable version
  id("org.jetbrains.kotlin.android") version "1.9.10" apply false // Compatible with Compose 1.5.3
  id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10" apply false // Kotlin serialization
  id("com.google.devtools.ksp") version "1.9.10-1.0.13" apply false // KSP for annotation processing
  id("androidx.room") version "2.7.0" apply false // Room plugin for schema export
  id("com.google.gms.google-services") version "4.4.0" apply false // Firebase App Distribution
  id("com.google.firebase.appdistribution") version "4.0.0" apply false // Firebase App Distribution plugin
  id("com.google.dagger.hilt.android") version "2.48" apply false // Hilt for dependency injection
}
