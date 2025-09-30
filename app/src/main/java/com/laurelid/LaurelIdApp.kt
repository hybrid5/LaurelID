package com.laurelid

import android.app.Application
import com.laurelid.util.Logger

class LaurelIdApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Logger.i("App", "LaurelID kiosk application initialized")
    }
}
