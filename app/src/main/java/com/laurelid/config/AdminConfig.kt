package com.laurelid.config

/**
 * Persisted configuration managed from the administrator settings panel.
 */
data class AdminConfig(
    val venueId: String = "",
    val trustRefreshIntervalMinutes: Int = DEFAULT_TRUST_REFRESH_MINUTES,
    val apiEndpointOverride: String = "",
    val demoMode: Boolean = false,
) {
    companion object {
        const val DEFAULT_TRUST_REFRESH_MINUTES = 60
    }
}
