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

        // Set up global exception handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                CrashLogger.logError("CRASH", "Uncaught exception in thread: ${thread.name}", throwable)
                CrashLogger.log("Log file saved to: ${CrashLogger.getLogPath()}")

                // Try to display error on screen
                runOnUiThread {
                    try {
                        gameView.showCrashError(throwable)
                    } catch (e: Exception) {
                        // Ignore if we can't show the error
                    }
                }

                // Give time for logging to complete
                Thread.sleep(500)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Call the original default handler to properly crash the app
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
