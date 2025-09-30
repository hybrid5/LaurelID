package com.laurelid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.laurelid.ui.ScannerActivity
import com.laurelid.util.Logger

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Intent.ACTION_BOOT_COMPLETED == intent?.action) {
            Logger.i(TAG, "Boot completed detected, launching scanner")
            val launchIntent = Intent(context, ScannerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(launchIntent)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
