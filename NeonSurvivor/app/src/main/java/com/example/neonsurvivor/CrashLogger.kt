package com.example.neonsurvivor

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Persistent crash logging utility for debugging
 */
object CrashLogger {

    private var logFile: File? = null
    private var context: Context? = null

    fun init(ctx: Context) {
        context = ctx

        // Try external files dir first, fallback to internal files dir
        val dir = ctx.getExternalFilesDir(null) ?: ctx.filesDir
        logFile = File(dir, "game_crash_log.txt")

        // Add session separator
        log("========================================")
        log("NEW SESSION STARTED")
        log("Log file: ${logFile?.absolutePath}")
        log("========================================")
    }

    fun log(message: String) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            val logMessage = "[$timestamp] $message\n"

            // Write to file
            logFile?.let { file ->
                FileWriter(file, true).use { writer ->
                    writer.append(logMessage)
                }
            }

            // Also log to logcat
            android.util.Log.d("CrashLogger", message)
        } catch (e: Exception) {
            android.util.Log.e("CrashLogger", "Failed to write log: ${e.message}")
        }
    }

    fun logError(tag: String, error: String, exception: Throwable? = null) {
        log("ERROR [$tag]: $error")
        exception?.let {
            log("Exception: ${it.message}")
            log("Stack trace: ${it.stackTraceToString()}")
        }
    }

    fun getLogPath(): String? {
        return logFile?.absolutePath
    }

    fun clearLog() {
        try {
            logFile?.writeText("")
            log("Log cleared")
        } catch (e: Exception) {
            android.util.Log.e("CrashLogger", "Failed to clear log: ${e.message}")
        }
    }

    fun getLogContents(): String {
        return try {
            logFile?.readText() ?: "No log file found"
        } catch (e: Exception) {
            "Error reading log: ${e.message}"
        }
    }
}
