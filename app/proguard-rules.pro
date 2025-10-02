# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Media3 Transformer rules
-keep class androidx.media3.** { *; }
-keep class androidx.media3.transformer.** { *; }
-keep class androidx.media3.effect.** { *; }
-keep class androidx.media3.common.** { *; }

# ML Kit Face Detection rules
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }

# Compose rules
-keep class androidx.compose.** { *; }

# Room Database rules
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *
-keep @androidx.room.TypeConverter class *
-keep class * extends androidx.room.RoomDatabase {
    public static <methods>;
}
-keep class androidx.room.migration.** { *; }

# PyTorch Mobile rules
-keep class org.pytorch.** { *; }
-keep class org.pytorch.torchvision.** { *; }
-keep class com.facebook.jni.** { *; }
-keep class com.facebook.soloader.** { *; }
-keep class com.facebook.jni.annotations.** { *; }

# PyTorch Mobile native libraries
-keep class org.pytorch.LiteModuleLoader { *; }
-keep class org.pytorch.Module { *; }
-keep class org.pytorch.Tensor { *; }
-keep class org.pytorch.IValue { *; }

# WorkManager rules
-keep class androidx.work.** { *; }
-keep class androidx.work.Worker { *; }
-keep class androidx.work.CoroutineWorker { *; }
-keep class androidx.work.WorkManager { *; }

# DataStore rules
-keep class androidx.datastore.** { *; }

# Mira specific rules
-keep class com.mira.videoeditor.** { *; }

# Suppress warnings
-dontwarn org.checkerframework.**
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.media3.**
-dontwarn androidx.room.**
-dontwarn org.pytorch.**
-dontwarn androidx.work.**
-dontwarn androidx.datastore.**

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
