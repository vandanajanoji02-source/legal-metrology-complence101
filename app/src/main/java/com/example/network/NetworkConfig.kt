package com.example.network

import android.content.Context
import android.content.SharedPreferences

/**
 * Network configuration manager for connecting Android to the FastAPI backend.
 *
 * Supported environments:
 * 1. Android Emulator loopback:
 *    http://10.0.2.2:5000/api/
 *
 * 2. Physical Android Phone connected to the same Wi-Fi / LAN:
 *    http://<LAPTOP_LAN_IP>:5000/api/
 *    (e.g., http://192.168.1.15:5000/api/)
 *
 * The active URL is stored in SharedPreferences and can be updated dynamically
 * at runtime via [setBaseUrl] or [setLanIp] without recompiling the APK.
 */
object NetworkConfig {
    const val DEFAULT_BASE_URL = "http://10.0.2.2:5000/api/"
    const val EMULATOR_BASE_URL = "http://10.0.2.2:5000/api/"
    const val PLACEHOLDER_LAN_URL = "http://<LAPTOP_LAN_IP>:5000/api/"
    const val DEFAULT_PORT = 5000

    private const val PREFS_NAME = "smartverify_network_prefs"
    private const val KEY_BASE_URL = "backend_api_base_url"

    private var cachedBaseUrl: String? = null
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Get the active backend Base URL.
     * Priority:
     * 1. In-memory cache
     * 2. SharedPreferences (configured via in-app dialog or [setBaseUrl]/[setLanIp])
     * 3. Default emulator loopback URL: http://10.0.2.2:5000/api/
     */
    fun getBaseUrl(context: Context? = null): String {
        cachedBaseUrl?.let { return it }

        val ctx = context ?: appContext
        if (ctx != null) {
            val prefs: SharedPreferences = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedUrl = prefs.getString(KEY_BASE_URL, null)
            if (!savedUrl.isNullOrBlank()) {
                val formatted = formatBaseUrl(savedUrl)
                cachedBaseUrl = formatted
                return formatted
            }
        }

        return DEFAULT_BASE_URL
    }

    /**
     * Set a custom backend URL (e.g., "http://192.168.1.15:5000/api/").
     */
    fun setBaseUrl(context: Context, newUrl: String) {
        val formatted = formatBaseUrl(newUrl)
        cachedBaseUrl = formatted
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BASE_URL, formatted).apply()
        ApiClient.rebuild()
    }

    /**
     * Helper to configure the backend using just the Laptop LAN IP.
     * Example: setLanIp(context, "192.168.1.15") -> sets "http://192.168.1.15:5000/api/"
     */
    fun setLanIp(context: Context, hostOrIp: String, port: Int = DEFAULT_PORT) {
        setBaseUrl(context, buildLanUrl(hostOrIp, port))
    }

    /**
     * Builds a full /api/ Base URL from an IP or hostname.
     */
    fun buildLanUrl(hostOrIp: String, port: Int = DEFAULT_PORT): String {
        val cleanHost = hostOrIp.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .trimEnd('/')
        val host = if (cleanHost.contains(":")) cleanHost else "$cleanHost:$port"
        return formatBaseUrl("http://$host")
    }

    /**
     * Reset configuration to default emulator loopback URL.
     */
    fun resetToDefault(context: Context) {
        cachedBaseUrl = DEFAULT_BASE_URL
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_BASE_URL).apply()
        ApiClient.rebuild()
    }

    /**
     * Normalizes and ensures the URL is well-formed with protocol and "/api/" suffix.
     * Handles inputs like:
     * - "192.168.1.10:5000" -> "http://192.168.1.10:5000/api/"
     * - "http://192.168.1.10:5000" -> "http://192.168.1.10:5000/api/"
     * - "http://10.0.2.2:5000/api/" -> "http://10.0.2.2:5000/api/"
     */
    fun formatBaseUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        clean = clean.trimEnd('/')
        if (!clean.endsWith("/api")) {
            clean = "$clean/api"
        }
        return "$clean/"
    }
}
