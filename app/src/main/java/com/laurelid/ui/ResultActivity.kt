package com.laurelid.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
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

        val root: ConstraintLayout = findViewById(R.id.resultRoot)
        val titleView: TextView = findViewById(R.id.resultTitle)
        val detailView: TextView = findViewById(R.id.resultDetail)
        val issuerView: TextView = findViewById(R.id.resultIssuer)
        val iconView: TextView = findViewById(R.id.resultIcon)

        if (success) {
            root.setBackgroundColor(ContextCompat.getColor(this, R.color.verification_success))
            iconView.text = "✓"
            titleView.text = getString(R.string.result_verified)
            detailView.text = getString(
                R.string.result_success_detail,
                if (ageOver21) "21+" else "Under 21"
            )
        } else {
            root.setBackgroundColor(ContextCompat.getColor(this, R.color.verification_failure))
            iconView.text = "✕"
            titleView.text = getString(R.string.result_rejected)
            detailView.text = getString(
                R.string.result_failure_detail,
                error ?: getString(R.string.result_details_error)
            )
        }

        issuerView.text = getString(R.string.result_issuer, issuer.ifEmpty { getString(R.string.result_unknown_issuer) })

        animateViews(iconView, titleView, detailView, issuerView)
    }

    private fun animateViews(vararg views: TextView) {
        val animations = views.flatMapIndexed { index, view ->
            view.alpha = 0f
            view.scaleX = 0.7f
            view.scaleY = 0.7f
            val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 1f)
            val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f)
            val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f)
            val startDelay = index * 80L
            listOf(alpha, scaleX, scaleY).onEach {
                it.duration = 250L
                it.startDelay = startDelay
            }
        }
        AnimatorSet().apply {
            playTogether(animations)
            start()
        }
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
        private const val RESULT_DISPLAY_DELAY_MS = 5000L
    }
}
