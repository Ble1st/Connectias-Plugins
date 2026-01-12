# Plugin ProGuard Rules

# Keep plugin class and methods
-keep class com.ble1st.connectias.networkinfoplugin.NetworkInfoPlugin {
    public *;
}

# Keep NetworkInfoService
-keep class com.ble1st.connectias.networkinfoplugin.NetworkInfoService {
    public *;
}

# Keep data classes
-keep class com.ble1st.connectias.networkinfoplugin.NetworkInfo {
    public *;
}
-keep class com.ble1st.connectias.networkinfoplugin.InterfaceInfo {
    public *;
}
-keep class com.ble1st.connectias.networkinfoplugin.WifiInfo {
    public *;
}

# Keep Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Keep Fragment
-keep class androidx.fragment.app.Fragment { *; }

# Keep plugin SDK interfaces
-keep interface com.ble1st.connectias.plugin.sdk.** { *; }
-keep class com.ble1st.connectias.plugin.sdk.** { *; }
