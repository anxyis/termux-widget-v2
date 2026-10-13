# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:/Users/$USER/AppData/Local/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-dontobfuscate
-dontoptimize

# Keep all widget and app classes
-keep class com.termux.widget.** { *; }
-keep class com.termux.shared.** { *; }

# Keep Android framework components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.appwidget.AppWidgetProvider
-keep public class * extends android.widget.RemoteViewsService
-keep public class * extends android.service.quicksettings.TileService
-keep public class * extends android.service.controls.ControlsProviderService

# Keep custom views used in XML
-keep class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Ignore warnings for optional library paths
-dontwarn com.google.common.**
-dontwarn io.reactivex.**
-dontwarn org.reactivestreams.**
