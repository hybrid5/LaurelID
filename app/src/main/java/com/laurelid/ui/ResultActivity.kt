package com.laurelid.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.laurelid.R
import com.laurelid.util.KioskUtil

class ResultActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        KioskUtil.applyKioskDecor(window)
        KioskUtil.blockBackPress(this)
        bindResult()
        scheduleReturn()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            KioskUtil.setImmersiveMode(window)
        }
    }

    override fun onBackPressed() {
        // Disable back navigation while in kiosk mode.
    }

    private fun bindResult() {
        val success = intent.getBooleanExtra(EXTRA_SUCCESS, false)
        val ageOver21 = intent.getBooleanExtra(EXTRA_AGE_OVER_21, false)
        val issuer = intent.getStringExtra(EXTRA_ISSUER).orEmpty()
        val error = intent.getStringExtra(EXTRA_ERROR)

        val titleView: TextView = findViewById(R.id.resultTitle)
        val detailView: TextView = findViewById(R.id.resultDetail)
        val issuerView: TextView = findViewById(R.id.resultIssuer)

        if (success) {
            titleView.text = getString(R.string.result_verified)
            titleView.setTextColor(ContextCompat.getColor(this, R.color.verification_success))
            detailView.text = getString(R.string.result_details_success, if (ageOver21) "21+" else "Under 21")
        } else {
            titleView.text = getString(R.string.result_rejected)
            titleView.setTextColor(ContextCompat.getColor(this, R.color.verification_failure))
            detailView.text = error ?: getString(R.string.result_details_error)
        }

        issuerView.text = getString(R.string.result_issuer, issuer.ifEmpty { getString(R.string.result_unknown_issuer) })
    }

    private fun scheduleReturn() {
        handler.postDelayed({
            val intent = Intent(this, ScannerActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }, RESULT_DISPLAY_DELAY_MS)
    }

    companion object {
        const val EXTRA_SUCCESS = "extra_success"
        const val EXTRA_AGE_OVER_21 = "extra_age_over_21"
        const val EXTRA_ISSUER = "extra_issuer"
        const val EXTRA_ERROR = "extra_error"
        private const val RESULT_DISPLAY_DELAY_MS = 4000L
    }
}
