package com.laurelid.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.laurelid.R
import com.laurelid.config.AdminConfig
import com.laurelid.config.ConfigManager
import com.laurelid.util.KioskUtil

class AdminActivity : AppCompatActivity() {

    private lateinit var configManager: ConfigManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        KioskUtil.applyKioskDecor(window)
        configManager = ConfigManager(applicationContext)
        bindConfig()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            KioskUtil.setImmersiveMode(window)
        }
    }

    private fun bindConfig() {
        val config = configManager.getConfig()
        val venueInput: EditText = findViewById(R.id.venueIdInput)
        val refreshInput: EditText = findViewById(R.id.trustRefreshInput)
        val apiInput: EditText = findViewById(R.id.apiEndpointInput)
        val demoSwitch: SwitchCompat = findViewById(R.id.demoModeSwitch)
        val saveButton: Button = findViewById(R.id.saveButton)

        venueInput.setText(config.venueId)
        refreshInput.setText(config.trustRefreshIntervalMinutes.toString())
        apiInput.setText(config.apiEndpointOverride)
        demoSwitch.isChecked = config.demoMode

        saveButton.setOnClickListener {
            val venueId = venueInput.text.toString().trim()
            val refreshMinutes = refreshInput.text.toString().toIntOrNull()
                ?: AdminConfig.DEFAULT_TRUST_REFRESH_MINUTES
            val endpoint = apiInput.text.toString().trim()
            val updatedConfig = AdminConfig(
                venueId = venueId,
                trustRefreshIntervalMinutes = refreshMinutes,
                apiEndpointOverride = endpoint,
                demoMode = demoSwitch.isChecked,
            )
            configManager.saveConfig(updatedConfig)
            Toast.makeText(this, R.string.admin_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
