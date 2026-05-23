# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve generic type signatures (essential for Gson's TypeToken) and annotations
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Keep Gson annotations and TypeToken subclasses
-keep class com.google.gson.annotations.** { *; }
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken

# Keep data models and network request/response classes to prevent obfuscation of JSON keys
-keep class com.eyalm.adns.data.network.** { *; }
-keep class com.eyalm.adns.data.models.** { *; }
-keep class com.eyalm.adns.data.Blocklist { *; }
-keep class com.eyalm.adns.data.ListItem { *; }
-keep class com.eyalm.adns.data.ToggleSetting { *; }
-keep class com.eyalm.adns.data.ListSetting { *; }
-keep class com.eyalm.adns.data.ListIcon { *; }