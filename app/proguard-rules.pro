# Regras ProGuard/R8 do MeetPen

# Preserva número de linha para stack traces legíveis em produção
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Vosk (usa JNA/reflection nativa) ─────────────────────────────────────────
-keep class org.vosk.** { *; }
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** { *; }
-dontwarn com.sun.jna.**

# ── TensorFlow Lite ──────────────────────────────────────────────────────────
-keep class org.tensorflow.** { *; }
-dontwarn org.tensorflow.**

# ── kotlinx.serialization ────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class br.com.meetpen.**$$serializer { *; }
-keepclassmembers class br.com.meetpen.** {
    *** Companion;
}
-keepclasseswithmembers class br.com.meetpen.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Retrofit / OkHttp / Okio ─────────────────────────────────────────────────
-keepattributes Signature, Exceptions
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
