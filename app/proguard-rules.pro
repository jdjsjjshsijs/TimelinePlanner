# Add project specific ProGuard rules here.
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Gson
-keep class com.example.timelineplanner.data.remote.** { *; }
-keep class com.example.timelineplanner.data.repository.AiResponse { *; }
-keep class com.example.timelineplanner.data.repository.AiOperation { *; }
