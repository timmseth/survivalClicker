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
    private var musicVolume = prefs.getFloat("music_volume", 0.12f)
    private var rainVolume = prefs.getFloat("rain_volume", 1.0f)
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
    private var backButtonRect = RectF()

    private var draggingMusicSlider = false
    private var draggingRainSlider = false

    // Game stats prefs for save/load
    private val gamePrefs = context.getSharedPreferences("game_stats", Context.MODE_PRIVATE)

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

        // Back button
        val buttonWidth = w * 0.7f
        val backButtonHeight = 120f
        val buttonCenterX = w / 2f

        backButtonRect = RectF(
            buttonCenterX - buttonWidth / 2f,
            h * 0.87f,
            buttonCenterX + buttonWidth / 2f,
            h * 0.87f + backButtonHeight
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
