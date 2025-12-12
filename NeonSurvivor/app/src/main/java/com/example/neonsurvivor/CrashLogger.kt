package com.example.neonsurvivor

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Persistent crash logging utility for debugging
 * CRITICAL: Every write MUST flush immediately to survive crashes
 */
object CrashLogger {

    private var logFile: File? = null
    private var context: Context? = null

    fun init(ctx: Context) {
        context = ctx

        // Use internal files dir (more reliable than external)
        val dir = ctx.filesDir
        logFile = File(dir, "game_crash_log.txt")

        // Add session separator
        log("========================================")
        log("NEW SESSION STARTED")
        log("Android Version: ${android.os.Build.VERSION.SDK_INT}")
        log("Device: ${android.os.Build.MODEL}")
        log("Log file: ${logFile?.absolutePath}")
        log("========================================")
    }

    fun log(message: String) {
        // Write to logcat immediately
        android.util.Log.i("CRASHLOG", message)

        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            val logMessage = "[$timestamp] $message\n"

            // CRITICAL: Use FileOutputStream with append mode and FLUSH immediately
            logFile?.let { file ->
                FileOutputStream(file, true).use { fos ->
                    fos.write(logMessage.toByteArray())
                    fos.flush() // Force write to disk NOW
                    fos.fd.sync() // Force OS to commit to physical storage
                }
            }
        } catch (e: Exception) {
            // Last resort - print to stderr which might show in crash logs
            System.err.println("CRASHLOG FAILED: $message")
            e.printStackTrace()
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
