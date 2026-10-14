# Google Play Services & Auth
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.**

# Firebase & Firestore Models
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.IgnoreExtraProperties <fields>;
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
}
-keep class com.example.data.UserBooking { *; }
-keep class com.example.data.UserAttendance { *; }
-keep class com.example.chat.model.** { *; }

# ZegoCloud UIKit
-keep class im.zego.** { *; }
-keep interface im.zego.** { *; }
-keep class com.zegocloud.** { *; }
-keep interface com.zegocloud.** { *; }
-dontwarn im.zego.**
-dontwarn com.zegocloud.**

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Moshi Generated Adapters
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}
-keep class *JsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
    public <init>(com.squareup.moshi.Moshi, java.lang.reflect.Type[]);
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}

# Glance App Widgets
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class com.example.widget.** { *; }

# Coil Image Loader
-dontwarn coil.**
-keep class coil.** { *; }

# Reflection-based Code: Kyant AndroidLiquidGlass in PopupBackdrop
-keep class com.kyant.backdrop.** { *; }
-keepclassmembers class com.kyant.backdrop.backdrops.LayerBackdrop { *; }

# BuildConfig Fields (Ensure ZEGO_APP_ID & ZEGO_APP_SIGN survive minify)
-keep class com.example.BuildConfig {
    public static final java.lang.String ZEGO_APP_ID;
    public static final java.lang.String ZEGO_APP_SIGN;
}

# OkHttp & Coroutines
-dontwarn okhttp3.**
-dontwarn okio.**


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
