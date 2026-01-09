# Plugin ProGuard Rules

# Keep plugin class and methods
-keep class com.ble1st.connectias.pingplugin.PingPlugin {
    public *;
}

# Keep PingService
-keep class com.ble1st.connectias.pingplugin.PingService {
    public *;
}

# Keep PingResult data class
-keep class com.ble1st.connectias.pingplugin.PingResult {
    public *;
}

# Keep commons-net classes
-keep class org.apache.commons.net.** { *; }
-dontwarn org.apache.commons.net.**

# Keep Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Keep Fragment
-keep class androidx.fragment.app.Fragment { *; }

# Keep plugin SDK interfaces
-keep interface com.ble1st.connectias.plugin.sdk.** { *; }
-keep class com.ble1st.connectias.plugin.sdk.** { *; }
