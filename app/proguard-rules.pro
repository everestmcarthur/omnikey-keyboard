# OmniKey ProGuard Rules

# Keep InputMethodService
-keep public class * extends android.inputmethodservice.InputMethodService

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep ML Kit models
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep data classes
-keep class com.omnkey.keyboard.core.model.** { *; }

# Keep coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# Keep service methods
-keepclassmembers class com.omnkey.keyboard.service.OmniKeyService {
    public <methods>;
}
