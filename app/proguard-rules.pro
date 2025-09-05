# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Gson
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.TypeAdapter { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class com.google.gson.Gson { *; }
-keep class com.google.gson.GsonBuilder { *; }
-keep class com.google.gson.JsonSerializer { *; }
-keep class com.google.gson.JsonDeserializer { *; }
-keep class com.google.gson.JsonElement { *; }
-keep class com.google.gson.JsonObject { *; }
-keep class com.google.gson.JsonArray { *; }
-keep class com.google.gson.JsonPrimitive { *; }

# Keep Gson type adapter factories
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep the API model classes and their members
-keep class com.example.mothership.api.model.** { *; }
-keepclassmembers class com.example.mothership.api.model.** { *; }

# Keep the MothershipApi interface and its methods
-keep class com.example.mothership.api.MothershipApi { *; }
-keepclassmembers class com.example.mothership.api.MothershipApi { *; }

# Keep generic type information
-keepattributes Signature
-keepattributes *Annotation*

# Keep the MainViewModel and related classes
-keep class com.example.mothership.MainViewModel { *; }
-keep class com.example.mothership.MainUiState { *; }
-keep class com.example.mothership.MainUiState$* { *; }

# Keep Application class
-keep class com.example.mothership.MothershipApp { *; }

# Keep Kotlin specific classes
-keep class kotlin.Metadata { *; }
-keep class kotlin.jvm.internal.** { *; }
-keep class kotlin.coroutines.** { *; }

# Keep annotation classes
-keep class com.google.gson.annotations.** { *; }

# Keep parameter names for reflection
-keepattributes ParameterName

# Keep the response classes and their generic types
-keep class retrofit2.Response { *; }
-keep class java.lang.reflect.ParameterizedType { *; }
-keep class java.lang.reflect.Type { *; }

# Keep the List interface and ArrayList implementation
-keep class java.util.List { *; }
-keep class java.util.ArrayList { *; }

# Keep DemoKeyManager inner data classes for Gson serialization
-keep class com.example.mothership.demo.DemoKeyManager$* { *; }

# Memory optimization rules
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# Keep memory-sensitive classes
-keep class com.example.mothership.** { *; }

# Reduce information kept for debugging to reduce memory usage
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers

# Keep ViewModel and related classes for proper memory management
-keep class androidx.lifecycle.ViewModel { *; }
-keep class androidx.lifecycle.AndroidViewModel { *; }
