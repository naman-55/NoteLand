# Add project specific ProGuard rules here.
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

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Glance Home Screen Widgets & ActionCallbacks
-keep class * implements androidx.glance.appwidget.action.ActionCallback { public <init>(); }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { public <init>(); }

# Room Database & Models
-keep class com.example.data.db.AppDatabase { *; }
-keep class com.example.data.model.** { *; }

# BroadcastReceivers, WorkManager Workers & Biometric Prompts
-keep class com.example.util.ReminderReceiver { public <init>(); }
-keep class com.example.worker.** { *; }
-keep class * extends androidx.work.ListenableWorker { public <init>(android.content.Context, androidx.work.WorkerParameters); }
-keep class androidx.biometric.** { *; }

# ViewModels, Entry Activity & BuildConfig
-keep class * extends androidx.lifecycle.AndroidViewModel { public <init>(android.app.Application); }
-keep class com.example.MainActivity { *; }
-keep class com.example.BuildConfig { *; }
