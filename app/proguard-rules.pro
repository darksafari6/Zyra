# Preserve runtime annotations and generic signatures used by Kotlin, Hilt and Room.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep class com.safari.zyra.core.data.chat.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn javax.annotation.**
