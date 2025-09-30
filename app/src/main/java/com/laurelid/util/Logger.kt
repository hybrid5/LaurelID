package com.laurelid.util

import android.util.Log

object Logger {
    private const val GLOBAL_TAG = "LaurelID"

    fun d(tag: String, message: String) {
        Log.d("$GLOBAL_TAG:$tag", message)
    }

    fun i(tag: String, message: String) {
        Log.i("$GLOBAL_TAG:$tag", message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w("$GLOBAL_TAG:$tag", message, throwable)
        } else {
            Log.w("$GLOBAL_TAG:$tag", message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("$GLOBAL_TAG:$tag", message, throwable)
        } else {
            Log.e("$GLOBAL_TAG:$tag", message)
        }
    }
}
