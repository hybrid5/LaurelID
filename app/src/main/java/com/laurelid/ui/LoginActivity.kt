package com.laurelid.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.laurelid.R
import com.laurelid.util.KioskUtil

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        KioskUtil.applyKioskDecor(window)
        KioskUtil.blockBackPress(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            KioskUtil.setImmersiveMode(window)
        }
    }
}
