package com.example.neonsurvivor

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.WindowManager

class MainActivity : Activity() {

    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize crash logger
        CrashLogger.init(this)
        CrashLogger.log("MainActivity onCreate")

        // Set up global exception handler - SIMPLE AND RELIABLE
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // Log the crash - this MUST complete
                CrashLogger.log("========================================")
                CrashLogger.log("FATAL CRASH DETECTED")
                CrashLogger.log("Thread: ${thread.name}")
                CrashLogger.log("Exception: ${throwable.javaClass.simpleName}")
                CrashLogger.log("Message: ${throwable.message}")
                CrashLogger.log("Stack trace:")
                throwable.stackTrace.forEach { element ->
                    CrashLogger.log("  at $element")
                }
                CrashLogger.log("Log saved to: ${CrashLogger.getLogPath()}")
                CrashLogger.log("========================================")
            } catch (e: Exception) {
                // If logging fails, print to stderr
                System.err.println("CRASH LOGGER FAILED!")
                e.printStackTrace()
            }

            // Let the system handle the crash
            defaultHandler?.uncaughtException(thread, throwable)
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        gameView = GameView(this)
        setContentView(gameView)
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
        // Stop audio when game loses focus
        AudioManager.stopMusic()
        AudioManager.stopRain()
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
        // Resume audio when game regains focus
        AudioManager.startMusic(this)
        AudioManager.startRain(this)
    }
}
