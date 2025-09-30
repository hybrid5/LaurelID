package com.laurelid.config

import android.content.Context
import androidx.core.content.edit
import kotlin.math.max

class ConfigManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getConfig(): AdminConfig {
        val venueId = prefs.getString(KEY_VENUE_ID, "").orEmpty()
        val refreshMinutes = prefs.getInt(KEY_REFRESH_MINUTES, AdminConfig.DEFAULT_TRUST_REFRESH_MINUTES)
        val endpoint = prefs.getString(KEY_API_ENDPOINT, "").orEmpty()
        val demoMode = prefs.getBoolean(KEY_DEMO_MODE, false)
        return AdminConfig(
            venueId = venueId,
            trustRefreshIntervalMinutes = sanitizeInterval(refreshMinutes),
            apiEndpointOverride = endpoint,
            demoMode = demoMode,
        )
    }

    fun saveConfig(config: AdminConfig) {
        prefs.edit {
            putString(KEY_VENUE_ID, config.venueId)
            putInt(KEY_REFRESH_MINUTES, sanitizeInterval(config.trustRefreshIntervalMinutes))
            putString(KEY_API_ENDPOINT, config.apiEndpointOverride)
            putBoolean(KEY_DEMO_MODE, config.demoMode)
        }
    }

    private fun sanitizeInterval(minutes: Int): Int {
        return max(1, minutes)
    }

    companion object {
        private const val PREFS_NAME = "laurelid_admin_config"
        private const val KEY_VENUE_ID = "venue_id"
        private const val KEY_REFRESH_MINUTES = "trust_refresh_minutes"
        private const val KEY_API_ENDPOINT = "api_endpoint"
        private const val KEY_DEMO_MODE = "demo_mode"
    }
}
