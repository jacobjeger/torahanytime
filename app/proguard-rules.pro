# Annotations
-keepattributes *Annotation*

# Moshi
-keep class com.torahanytime.audio.data.model.** { *; }
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class **JsonAdapter { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}

# Retrofit
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep class com.torahanytime.audio.data.api.TATApiService { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Room entities
-keep class com.torahanytime.audio.data.local.entity.** { *; }

# Media3 service
-keep class com.torahanytime.audio.player.AudioPlayerService { *; }
-keep class androidx.media3.session.** { *; }

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Encrypted SharedPreferences
-keep class androidx.security.crypto.** { *; }
