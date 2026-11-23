# ProGuard & R8 Configuration for LingoSphere
# Optimizations and shrinker rules for release builds

# -----------------------------------------------------------------------------
# General Android & Kotlin Configuration
# -----------------------------------------------------------------------------
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Kotlin Reflection & Metadata
-dontwarn kotlin.reflect.**
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# -----------------------------------------------------------------------------
# Firebase & Google Play Services Rules
# -----------------------------------------------------------------------------
# Firebase Common, Firestore, Auth, and AI Logic
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Firestore object serialization & custom field mappings
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.Exclude <fields>;
    @com.google.firebase.firestore.Exclude <methods>;
    @com.google.firebase.firestore.IgnoreExtraProperties class *;
}

# Keep no-arg constructors and accessors for Firestore entities
-keepclassmembers class com.example.data.model.** {
    <init>();
    <fields>;
    <methods>;
}

# -----------------------------------------------------------------------------
# Retrofit & OkHttp Rules
# -----------------------------------------------------------------------------
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Preserve Retrofit interfaces, method signatures, and annotations
-keepattributes EnclosingMethod,Signature
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp public suffix database and platform socket security
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# -----------------------------------------------------------------------------
# Moshi Rules
# -----------------------------------------------------------------------------
-dontwarn com.squareup.moshi.**
-dontwarn java.lang.invoke.MethodHandles$Lookup

# Preserve Moshi's generated JsonAdapters
-keep class *JsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
    public <init>(com.squareup.moshi.Moshi, java.lang.reflect.Type[]);
    public <init>(...);
}

# Preserve classes annotated with @JsonClass and their primary constructors
-keep @com.squareup.moshi.JsonClass class * {
    <init>(...);
    <fields>;
}

# Preserve fields and methods annotated with @Json
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.Json <methods>;
}

# Preserve custom JsonQualifiers
-keep @com.squareup.moshi.JsonQualifier @interface *

# -----------------------------------------------------------------------------
# Application Data & DTO Models
# -----------------------------------------------------------------------------
-keep class com.example.data.model.** { *; }
-keep class com.example.ai.** { *; }
-keep class com.example.avatar.** { *; }
-keep class com.example.speech.** { *; }

# Keep Enum values and valueOf helpers
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# -----------------------------------------------------------------------------
# Coil Image Loader
# -----------------------------------------------------------------------------
-dontwarn coil.**
-keep class coil.** { *; }

# -----------------------------------------------------------------------------
# Android WebView & JS Interface
# -----------------------------------------------------------------------------
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
