package com.laurelid.util

import android.content.Context
import com.laurelid.config.AdminConfig
import com.laurelid.data.VerificationResult
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.TimeUnit
import org.json.JSONObject

object LogManager {
    private const val LOG_DIR = "logs"
    private const val LOG_FILE = "verify.log"
    private val TIMESTAMP_REGEX = Regex("\"ts\":(\\d+)")

    fun appendVerification(context: Context, result: VerificationResult, config: AdminConfig, demoModeUsed: Boolean) {
        try {
            val dir = File(context.filesDir, LOG_DIR)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, LOG_FILE)
            val payload = JSONObject().apply {
                put("ts", System.currentTimeMillis())
                put("venueId", config.venueId)
                put("success", result.success)
                put("subjectDid", result.subjectDid)
                put("docType", result.docType)
                put("issuer", result.issuer)
                put("ageOver21", result.ageOver21)
                put("error", result.error)
                put("demoMode", demoModeUsed)
            }.toString()
            FileWriter(file, true).use { writer ->
                writer.appendLine(payload)
            }
        } catch (ioException: IOException) {
            Logger.e(TAG, "Unable to append verification log", ioException)
        }
    }

    fun purgeOldLogs(context: Context) {
        val logFile = File(File(context.filesDir, LOG_DIR), LOG_FILE)
        if (!logFile.exists()) return
        val lines = try {
            logFile.readLines()
        } catch (ioException: IOException) {
            Logger.e(TAG, "Unable to read verification logs for retention", ioException)
            return
        }
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        val filtered = lines.filter { line ->
            val timestamp = TIMESTAMP_REGEX.find(line)?.groupValues?.getOrNull(1)?.toLongOrNull()
            timestamp == null || timestamp >= cutoff
        }
        if (filtered.size == lines.size) {
            return
        }
        try {
            if (filtered.isEmpty()) {
                logFile.delete()
            } else {
                logFile.writeText(filtered.joinToString(separator = "\n", postfix = "\n"))
            }
            Logger.i(TAG, "Purged verification logs older than 30 days")
        } catch (ioException: IOException) {
            Logger.e(TAG, "Unable to rewrite verification logs", ioException)
        }
    }

    private const val TAG = "LogManager"
}
