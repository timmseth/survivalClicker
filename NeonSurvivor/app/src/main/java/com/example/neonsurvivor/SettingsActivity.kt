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
    private var backButtonRect = RectF()

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
            startX, h * 0.35f,
            startX + toggleWidth, h * 0.35f + toggleHeight
        )

        soundToggleRect = RectF(
            startX, h * 0.50f,
            startX + toggleWidth, h * 0.50f + toggleHeight
        )

        // Back button
        val buttonWidth = w * 0.7f
        val buttonHeight = 120f
        val buttonCenterX = w / 2f

        backButtonRect = RectF(
            buttonCenterX - buttonWidth / 2f,
            h * 0.75f,
            buttonCenterX + buttonWidth / 2f,
            h * 0.75f + buttonHeight
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

        // Back button
        canvas.drawRoundRect(backButtonRect, 20f, 20f, buttonBgPaint)
        canvas.drawRoundRect(backButtonRect, 20f, 20f, buttonBorderPaint)
        titlePaint.textSize = 60f
        canvas.drawText("BACK", backButtonRect.centerX(), backButtonRect.centerY() + 20f, titlePaint)
        titlePaint.textSize = 80f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x
            val y = event.y

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
                backButtonRect.contains(x, y) -> {
                    (context as Activity).finish()
                    return true
                }
            }
        }
        return true
    }
}
