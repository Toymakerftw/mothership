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
-keep class com.toymakerftw.mothership.api.model.** { *; }
-keepclassmembers class com.toymakerftw.mothership.api.model.** { *; }

# Keep the MothershipApi interface and its methods
-keep class com.toymakerftw.mothership.api.MothershipApi { *; }
-keepclassmembers class com.toymakerftw.mothership.api.MothershipApi { *; }

# Keep generic type information
-keepattributes Signature
-keepattributes *Annotation*

# Keep the MainViewModel and related classes
-keep class com.toymakerftw.mothership.MainViewModel { *; }
-keep class com.toymakerftw.mothership.MainUiState { *; }
-keep class com.toymakerftw.mothership.MainUiState$* { *; }

# Keep Application class
-keep class com.toymakerftw.mothership.MothershipApp { *; }

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
-keep class com.toymakerftw.mothership.demo.DemoKeyManager$* { *; }

# Memory optimization rules
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# Keep memory-sensitive classes
-keep class com.toymakerftw.mothership.** { *; }

# Reduce information kept for debugging to reduce memory usage
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers

# Keep ViewModel and related classes for proper memory management
-keep class androidx.lifecycle.ViewModel { *; }
-keep class androidx.lifecycle.AndroidViewModel { *; }

# NanoHTTPD rules
-keep class fi.iki.elonen.NanoHTTPD { *; }
-keep class fi.iki.elonen.NanoHTTPD** { *; }
-keep class org.nanohttpd.** { *; }

# Keep PWA HTTP server service
-keep class com.toymakerftw.mothership.PwaHttpServerService { *; }
-keep class com.toymakerftw.mothership.PwaHttpServerService$PwaHttpServer { *; }

# Keep file I/O classes used by the HTTP server
-keep class java.io.FileInputStream { *; }
-keep class java.io.File { *; }

# zip4j
-keep public class net.lingala.zip4j.** { *; }
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keep class net.lingala.zip4j.model.** { *; }
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-dontwarn net.lingala.zip4j.**