package com.example.neonsurvivor

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.*

class SettingsActivity : Activity() {

    private lateinit var settingsView: SettingsView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        settingsView = SettingsView(this)
        setContentView(settingsView)
    }
}

class SettingsView(context: Context) : View(context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private var musicEnabled = prefs.getBoolean("music_enabled", true)
    private var soundEnabled = prefs.getBoolean("sound_enabled", true)
    private var musicVolume = prefs.getFloat("music_volume", 0.5f)  // Default 50% volume
    private var rainVolume = prefs.getFloat("rain_volume", 0.3f)  // Default 30% for ambient sound
    private var powerupsEnabled = prefs.getBoolean("powerups_enabled", true)

    // Paints
    private val bgPaint = Paint()
    private val titlePaint = Paint().apply {
        color = Color.CYAN
        textSize = 80f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isAntiAlias = true
    }

    private val labelPaint = Paint().apply {
        color = Color.WHITE
        textSize = 50f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isAntiAlias = true
    }

    private val buttonBgPaint = Paint().apply {
        color = Color.argb(150, 20, 20, 30)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val buttonBorderPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val toggleOnPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val toggleOffPaint = Paint().apply {
        color = Color.argb(100, 100, 100, 100)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // UI elements
    private var musicToggleRect = RectF()
    private var soundToggleRect = RectF()
    private var powerupsToggleRect = RectF()
    private var musicVolumeSliderRect = RectF()
    private var rainVolumeSliderRect = RectF()
    private var saveButtonRect = RectF()
    private var loadButtonRect = RectF()
    private var viewLogButtonRect = RectF()
    private var debugButtonRect = RectF()
    private var backButtonRect = RectF()

    // Debug settings
    private var godModeToggleRect = RectF()
    private var spriteMenuToggleRect = RectF()
    private var bossMenuToggleRect = RectF()
    private var gunCountMinusRect = RectF()
    private var gunCountPlusRect = RectF()
    private var showingDebugMenu = false

    private var draggingMusicSlider = false
    private var draggingRainSlider = false

    // Game stats prefs for save/load
    private val gamePrefs = context.getSharedPreferences("game_stats", Context.MODE_PRIVATE)
    private val debugPrefs = context.getSharedPreferences("debug_settings", Context.MODE_PRIVATE)

    private var godMode = debugPrefs.getBoolean("god_mode", false)
    private var spriteMenuEnabled = debugPrefs.getBoolean("sprite_menu_enabled", false)
    private var bossMenuEnabled = debugPrefs.getBoolean("boss_menu_enabled", false)
    private var gunCount = debugPrefs.getInt("gun_count", 1)

    init {
        isFocusable = true
        isClickable = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Background gradient
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(
                Color.rgb(5, 5, 15),
                Color.rgb(10, 5, 20),
                Color.rgb(15, 10, 25)
            ),
            null,
            Shader.TileMode.CLAMP
        )

        // Position toggles
        val toggleWidth = 200f
        val toggleHeight = 80f
        val startX = w * 0.6f

        musicToggleRect = RectF(
            startX, h * 0.30f,
            startX + toggleWidth, h * 0.30f + toggleHeight
        )

        soundToggleRect = RectF(
            startX, h * 0.42f,
            startX + toggleWidth, h * 0.42f + toggleHeight
        )

        powerupsToggleRect = RectF(
            startX, h * 0.54f,
            startX + toggleWidth, h * 0.54f + toggleHeight
        )

        // Volume sliders (tripled height for easier touch)
        val sliderWidth = w * 0.5f
        val sliderHeight = 60f
        val sliderStartX = w * 0.3f

        musicVolumeSliderRect = RectF(
            sliderStartX, h * 0.36f,
            sliderStartX + sliderWidth, h * 0.36f + sliderHeight
        )

        rainVolumeSliderRect = RectF(
            sliderStartX, h * 0.48f,
            sliderStartX + sliderWidth, h * 0.48f + sliderHeight
        )

        // Save/Load buttons (side by side)
        val smallButtonWidth = w * 0.35f
        val buttonHeight = 100f

        saveButtonRect = RectF(
            w * 0.1f,
            h * 0.73f,
            w * 0.1f + smallButtonWidth,
            h * 0.73f + buttonHeight
        )

        loadButtonRect = RectF(
            w * 0.55f,
            h * 0.73f,
            w * 0.55f + smallButtonWidth,
            h * 0.73f + buttonHeight
        )

        // View Log and Debug buttons (side by side below save/load)
        val debugButtonWidth = w * 0.35f
        viewLogButtonRect = RectF(
            w * 0.1f,
            h * 0.85f,
            w * 0.1f + debugButtonWidth,
            h * 0.85f + 80f
        )

        debugButtonRect = RectF(
            w * 0.55f,
            h * 0.85f,
            w * 0.55f + debugButtonWidth,
            h * 0.85f + 80f
        )

        // Debug menu controls
        val debugToggleWidth = 150f
        val debugToggleHeight = 60f
        godModeToggleRect = RectF(
            w * 0.6f, h * 0.25f,
            w * 0.6f + debugToggleWidth, h * 0.25f + debugToggleHeight
        )

        spriteMenuToggleRect = RectF(
            w * 0.6f, h * 0.33f,
            w * 0.6f + debugToggleWidth, h * 0.33f + debugToggleHeight
        )

        bossMenuToggleRect = RectF(
            w * 0.6f, h * 0.41f,
            w * 0.6f + debugToggleWidth, h * 0.41f + debugToggleHeight
        )

        val buttonSize = 60f
        gunCountMinusRect = RectF(
            w * 0.4f, h * 0.51f,
            w * 0.4f + buttonSize, h * 0.51f + buttonSize
        )
        gunCountPlusRect = RectF(
            w * 0.7f, h * 0.51f,
            w * 0.7f + buttonSize, h * 0.51f + buttonSize
        )

        // Back button
        val buttonWidth = w * 0.7f
        val backButtonHeight = 120f
        val buttonCenterX = w / 2f

        backButtonRect = RectF(
            buttonCenterX - buttonWidth / 2f,
            h * 0.94f,
            buttonCenterX + buttonWidth / 2f,
            h * 0.94f + backButtonHeight
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        // Background
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Title
        canvas.drawText("SETTINGS", w / 2f, h * 0.15f, titlePaint)

        // Music toggle
        canvas.drawText("MUSIC", w * 0.25f, musicToggleRect.centerY() + 18f, labelPaint)
        canvas.drawRoundRect(musicToggleRect, 40f, 40f, if (musicEnabled) toggleOnPaint else toggleOffPaint)
        canvas.drawRoundRect(musicToggleRect, 40f, 40f, buttonBorderPaint)
        labelPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(if (musicEnabled) "ON" else "OFF",
            musicToggleRect.centerX(), musicToggleRect.centerY() + 18f, labelPaint)
        labelPaint.textAlign = Paint.Align.LEFT

        // Sound toggle
        canvas.drawText("SOUND", w * 0.25f, soundToggleRect.centerY() + 18f, labelPaint)
        canvas.drawRoundRect(soundToggleRect, 40f, 40f, if (soundEnabled) toggleOnPaint else toggleOffPaint)
        canvas.drawRoundRect(soundToggleRect, 40f, 40f, buttonBorderPaint)
        labelPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(if (soundEnabled) "ON" else "OFF",
            soundToggleRect.centerX(), soundToggleRect.centerY() + 18f, labelPaint)
        labelPaint.textAlign = Paint.Align.LEFT

        // Powerups toggle
        canvas.drawText("POWERUPS", w * 0.25f, powerupsToggleRect.centerY() + 18f, labelPaint)
        canvas.drawRoundRect(powerupsToggleRect, 40f, 40f, if (powerupsEnabled) toggleOnPaint else toggleOffPaint)
        canvas.drawRoundRect(powerupsToggleRect, 40f, 40f, buttonBorderPaint)
        labelPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(if (powerupsEnabled) "ON" else "OFF",
            powerupsToggleRect.centerX(), powerupsToggleRect.centerY() + 18f, labelPaint)
        labelPaint.textAlign = Paint.Align.LEFT

        // Music volume slider
        val sliderLabelPaint = Paint().apply {
            color = Color.WHITE
            textSize = 35f
            isAntiAlias = true
        }
        canvas.drawText("Music Vol: ${(musicVolume * 100).toInt()}%",
            w * 0.3f, musicVolumeSliderRect.top - 10f, sliderLabelPaint)

        // Slider track
        canvas.drawRoundRect(musicVolumeSliderRect, 10f, 10f, buttonBgPaint)
        canvas.drawRoundRect(musicVolumeSliderRect, 10f, 10f, buttonBorderPaint)

        // Slider fill
        val musicFillWidth = musicVolumeSliderRect.width() * musicVolume
        canvas.drawRoundRect(
            musicVolumeSliderRect.left, musicVolumeSliderRect.top,
            musicVolumeSliderRect.left + musicFillWidth, musicVolumeSliderRect.bottom,
            10f, 10f, toggleOnPaint
        )

        // Rain volume slider
        canvas.drawText("Rain Vol: ${(rainVolume * 100).toInt()}%",
            w * 0.3f, rainVolumeSliderRect.top - 10f, sliderLabelPaint)

        // Slider track
        canvas.drawRoundRect(rainVolumeSliderRect, 10f, 10f, buttonBgPaint)
        canvas.drawRoundRect(rainVolumeSliderRect, 10f, 10f, buttonBorderPaint)

        // Slider fill
        val rainFillWidth = rainVolumeSliderRect.width() * rainVolume
        canvas.drawRoundRect(
            rainVolumeSliderRect.left, rainVolumeSliderRect.top,
            rainVolumeSliderRect.left + rainFillWidth, rainVolumeSliderRect.bottom,
            10f, 10f, toggleOnPaint
        )

        // Save/Load buttons
        val smallTextPaint = Paint().apply {
            color = Color.CYAN
            textSize = 45f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        // Check if save exists
        val hasSave = gamePrefs.getInt("saved_wave", 0) > 0

        canvas.drawRoundRect(saveButtonRect, 15f, 15f, buttonBgPaint)
        canvas.drawRoundRect(saveButtonRect, 15f, 15f, buttonBorderPaint)
        canvas.drawText("SAVE", saveButtonRect.centerX(), saveButtonRect.centerY() + 15f, smallTextPaint)

        val loadBorderPaint = Paint().apply {
            color = if (hasSave) Color.CYAN else Color.GRAY
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }
        val loadTextPaint = Paint().apply {
            color = if (hasSave) Color.CYAN else Color.GRAY
            textSize = 45f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawRoundRect(loadButtonRect, 15f, 15f, buttonBgPaint)
        canvas.drawRoundRect(loadButtonRect, 15f, 15f, loadBorderPaint)
        canvas.drawText("LOAD", loadButtonRect.centerX(), loadButtonRect.centerY() + 15f, loadTextPaint)

        // View Log button
        val logButtonPaint = Paint().apply {
            color = Color.YELLOW
            textSize = 35f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val logBorderPaint = Paint().apply {
            color = Color.YELLOW
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        canvas.drawRoundRect(viewLogButtonRect, 15f, 15f, buttonBgPaint)
        canvas.drawRoundRect(viewLogButtonRect, 15f, 15f, logBorderPaint)
        canvas.drawText("LOG", viewLogButtonRect.centerX(), viewLogButtonRect.centerY() + 12f, logButtonPaint)

        // Debug button
        val debugBorderPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        val debugTextPaint = Paint().apply {
            color = Color.RED
            textSize = 35f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawRoundRect(debugButtonRect, 15f, 15f, buttonBgPaint)
        canvas.drawRoundRect(debugButtonRect, 15f, 15f, debugBorderPaint)
        canvas.drawText("DEBUG", debugButtonRect.centerX(), debugButtonRect.centerY() + 12f, debugTextPaint)

        // Debug menu (if showing)
        if (showingDebugMenu) {
            // Semi-transparent overlay
            val overlayPaint = Paint().apply {
                color = Color.argb(220, 10, 10, 20)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, w, h, overlayPaint)

            // Debug menu title
            val debugTitlePaint = Paint().apply {
                color = Color.RED
                textSize = 60f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("DEBUG MENU", w / 2f, h * 0.2f, debugTitlePaint)

            // Toggle text paint
            val toggleTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 30f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            val smallLabelPaint = Paint().apply {
                color = Color.WHITE
                textSize = 38f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            // God mode toggle
            canvas.drawText("GOD MODE", w * 0.18f, godModeToggleRect.centerY() + 15f, smallLabelPaint)
            canvas.drawRoundRect(godModeToggleRect, 30f, 30f, if (godMode) toggleOnPaint else toggleOffPaint)
            canvas.drawRoundRect(godModeToggleRect, 30f, 30f, buttonBorderPaint)
            canvas.drawText(if (godMode) "ON" else "OFF",
                godModeToggleRect.centerX(), godModeToggleRect.centerY() + 12f, toggleTextPaint)

            // Sprite Menu toggle
            canvas.drawText("SPRITE MENU", w * 0.15f, spriteMenuToggleRect.centerY() + 15f, smallLabelPaint)
            canvas.drawRoundRect(spriteMenuToggleRect, 30f, 30f, if (spriteMenuEnabled) toggleOnPaint else toggleOffPaint)
            canvas.drawRoundRect(spriteMenuToggleRect, 30f, 30f, buttonBorderPaint)
            canvas.drawText(if (spriteMenuEnabled) "ON" else "OFF",
                spriteMenuToggleRect.centerX(), spriteMenuToggleRect.centerY() + 12f, toggleTextPaint)

            // Boss Menu toggle
            canvas.drawText("BOSS MENU", w * 0.17f, bossMenuToggleRect.centerY() + 15f, smallLabelPaint)
            canvas.drawRoundRect(bossMenuToggleRect, 30f, 30f, if (bossMenuEnabled) toggleOnPaint else toggleOffPaint)
            canvas.drawRoundRect(bossMenuToggleRect, 30f, 30f, buttonBorderPaint)
            canvas.drawText(if (bossMenuEnabled) "ON" else "OFF",
                bossMenuToggleRect.centerX(), bossMenuToggleRect.centerY() + 12f, toggleTextPaint)

            // Gun count controls
            canvas.drawText("GUN COUNT", w * 0.18f, gunCountMinusRect.centerY() + 15f, smallLabelPaint)

            // Minus button
            canvas.drawRoundRect(gunCountMinusRect, 10f, 10f, buttonBgPaint)
            canvas.drawRoundRect(gunCountMinusRect, 10f, 10f, buttonBorderPaint)
            canvas.drawText("-", gunCountMinusRect.centerX(), gunCountMinusRect.centerY() + 18f, debugTitlePaint)

            // Gun count display
            canvas.drawText("$gunCount", w * 0.55f, gunCountMinusRect.centerY() + 15f, labelPaint)

            // Plus button
            canvas.drawRoundRect(gunCountPlusRect, 10f, 10f, buttonBgPaint)
            canvas.drawRoundRect(gunCountPlusRect, 10f, 10f, buttonBorderPaint)
            canvas.drawText("+", gunCountPlusRect.centerX(), gunCountPlusRect.centerY() + 18f, debugTitlePaint)

            // Close button
            val closeButtonRect = RectF(w * 0.25f, h * 0.7f, w * 0.75f, h * 0.8f)
            canvas.drawRoundRect(closeButtonRect, 20f, 20f, buttonBgPaint)
            canvas.drawRoundRect(closeButtonRect, 20f, 20f, buttonBorderPaint)
            canvas.drawText("CLOSE", closeButtonRect.centerX(), closeButtonRect.centerY() + 20f, debugTitlePaint)
        }

        // Back button
        canvas.drawRoundRect(backButtonRect, 20f, 20f, buttonBgPaint)
        canvas.drawRoundRect(backButtonRect, 20f, 20f, buttonBorderPaint)
        titlePaint.textSize = 60f
        canvas.drawText("BACK", backButtonRect.centerX(), backButtonRect.centerY() + 20f, titlePaint)
        titlePaint.textSize = 80f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (musicVolumeSliderRect.contains(x, y)) {
                    draggingMusicSlider = true
                    updateMusicVolume(x)
                    return true
                }
                if (rainVolumeSliderRect.contains(x, y)) {
                    draggingRainSlider = true
                    updateRainVolume(x)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (draggingMusicSlider) {
                    updateMusicVolume(x)
                    return true
                }
                if (draggingRainSlider) {
                    updateRainVolume(x)
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                draggingMusicSlider = false
                draggingRainSlider = false

                // Check debug menu buttons FIRST (if showing) so they take precedence
                if (showingDebugMenu) {
                    when {
                        godModeToggleRect.contains(x, y) -> {
                            godMode = !godMode
                            debugPrefs.edit().putBoolean("god_mode", godMode).apply()
                            invalidate()
                            return true
                        }
                        spriteMenuToggleRect.contains(x, y) -> {
                            spriteMenuEnabled = !spriteMenuEnabled
                            debugPrefs.edit().putBoolean("sprite_menu_enabled", spriteMenuEnabled).apply()
                            invalidate()
                            return true
                        }
                        bossMenuToggleRect.contains(x, y) -> {
                            bossMenuEnabled = !bossMenuEnabled
                            debugPrefs.edit().putBoolean("boss_menu_enabled", bossMenuEnabled).apply()
                            invalidate()
                            return true
                        }
                        gunCountMinusRect.contains(x, y) -> {
                            gunCount = (gunCount - 1).coerceAtLeast(1)
                            debugPrefs.edit().putInt("gun_count", gunCount).apply()
                            invalidate()
                            return true
                        }
                        gunCountPlusRect.contains(x, y) -> {
                            gunCount = (gunCount + 1).coerceAtMost(10)
                            debugPrefs.edit().putInt("gun_count", gunCount).apply()
                            invalidate()
                            return true
                        }
                        RectF(width.toFloat() * 0.25f, height.toFloat() * 0.7f, width.toFloat() * 0.75f, height.toFloat() * 0.8f).contains(x, y) -> {
                            // Close debug menu
                            showingDebugMenu = false
                            invalidate()
                            return true
                        }
                    }
                    // If debug menu is showing and we didn't hit any debug buttons, consume the event anyway
                    // to prevent clicking through to main menu
                    return true
                }

                // Main menu buttons (only if debug menu not showing)
                when {
                    musicToggleRect.contains(x, y) -> {
                        musicEnabled = !musicEnabled
                        prefs.edit().putBoolean("music_enabled", musicEnabled).apply()

                        // Toggle music
                        if (musicEnabled) {
                            AudioManager.startMusic(context)
                        } else {
                            AudioManager.stopMusic()
                        }
                        invalidate()
                        return true
                    }
                    soundToggleRect.contains(x, y) -> {
                        soundEnabled = !soundEnabled
                        prefs.edit().putBoolean("sound_enabled", soundEnabled).apply()

                        // Toggle rain sound
                        if (soundEnabled) {
                            AudioManager.startRain(context)
                        } else {
                            AudioManager.stopRain()
                        }
                        invalidate()
                        return true
                    }
                    powerupsToggleRect.contains(x, y) -> {
                        powerupsEnabled = !powerupsEnabled
                        prefs.edit().putBoolean("powerups_enabled", powerupsEnabled).apply()
                        invalidate()
                        return true
                    }
                    saveButtonRect.contains(x, y) -> {
                        // Trigger save in GameView (via broadcast or shared pref flag)
                        gamePrefs.edit().putBoolean("trigger_save", true).apply()
                        invalidate() // Refresh to show save exists
                        return true
                    }
                    loadButtonRect.contains(x, y) -> {
                        // Only allow load if save exists
                        if (gamePrefs.getInt("saved_wave", 0) > 0) {
                            gamePrefs.edit().putBoolean("trigger_load", true).apply()
                            (context as Activity).finish()
                        }
                        return true
                    }
                    viewLogButtonRect.contains(x, y) -> {
                        // Show crash log in a dialog
                        val logContents = CrashLogger.getLogContents()
                        val logPath = CrashLogger.getLogPath()
                        android.app.AlertDialog.Builder(context)
                            .setTitle("Crash Log")
                            .setMessage("Log file: $logPath\n\n$logContents")
                            .setPositiveButton("OK", null)
                            .setNeutralButton("Clear Log") { _, _ ->
                                CrashLogger.clearLog()
                            }
                            .show()
                        return true
                    }
                    debugButtonRect.contains(x, y) -> {
                        showingDebugMenu = true
                        invalidate()
                        return true
                    }
                    backButtonRect.contains(x, y) -> {
                        (context as Activity).finish()
                        return true
                    }
                }
            }
        }
        return true
    }

    private fun updateMusicVolume(x: Float) {
        val ratio = ((x - musicVolumeSliderRect.left) / musicVolumeSliderRect.width()).coerceIn(0f, 1f)
        musicVolume = ratio
        AudioManager.updateMusicVolume(musicVolume)
        prefs.edit().putFloat("music_volume", musicVolume).apply()
        invalidate()
    }

    private fun updateRainVolume(x: Float) {
        val ratio = ((x - rainVolumeSliderRect.left) / rainVolumeSliderRect.width()).coerceIn(0f, 1f)
        rainVolume = ratio
        AudioManager.rainVolume = rainVolume
        prefs.edit().putFloat("rain_volume", rainVolume).apply()
        invalidate()
    }
}
