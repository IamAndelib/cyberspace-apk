# Keep LauncherActivity and all inner classes
-keep class online.cyberspace.twa.LauncherActivity { *; }
-keep class online.cyberspace.twa.LauncherActivity$* { *; }

# Keep WebView-related classes
-keep public class android.webkit.WebView
-keep public class android.webkit.WebViewClient
-keep public class android.webkit.WebChromeClient
-keep public class * extends android.webkit.WebViewClient
-keep public class * extends android.webkit.WebChromeClient

# Keep @JavascriptInterface annotated methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# AndroidX Browser
-keep class androidx.browser.** { *; }

# Keep DownloadManager usage
-keep class android.app.DownloadManager { *; }
-keep class android.app.DownloadManager$Request { *; }

# Standard Android rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Prevent stripping ValueCallback used for file chooser
-keep class android.webkit.ValueCallback { *; }
