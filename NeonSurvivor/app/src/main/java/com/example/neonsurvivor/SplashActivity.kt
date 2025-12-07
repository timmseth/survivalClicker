package com.example.neonsurvivor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.*
import kotlin.random.Random

class SplashActivity : Activity() {

    private lateinit var splashView: SplashScreenView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        splashView = SplashScreenView(this)
        setContentView(splashView)
    }
}

class SplashScreenView(context: Context) : View(context) {

        private var lastTimeNs: Long = System.nanoTime()
        private var elapsedTime = 0f

        // Background gradient
        private val bgPaint = Paint()

        // Neon text paints
        private val neonGlowPaint = Paint().apply {
            color = Color.CYAN
            style = Paint.Style.FILL
            textSize = 140f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(50f, 0f, 0f, Color.CYAN)
        }

        private val neonCorePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            textSize = 140f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        private val neonOutlinePaint = Paint().apply {
            color = Color.rgb(0, 200, 255)
            style = Paint.Style.STROKE
            strokeWidth = 10f
            textSize = 140f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        // Button paints
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
            setShadowLayer(20f, 0f, 0f, Color.CYAN)
        }

        private val buttonTextPaint = Paint().apply {
            color = Color.CYAN
            textSize = 60f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        // Rain particles
        data class RainDrop(
            var x: Float,
            var y: Float,
            val speed: Float,
            val length: Float,
            val alpha: Int,
            val layer: Int
        )

        private val rainDrops = mutableListOf<RainDrop>()
        private val rainPaint = Paint().apply {
            style = Paint.Style.FILL
            strokeCap = Paint.Cap.ROUND
        }

        private val scanLinePaint = Paint().apply {
            color = Color.argb(15, 0, 255, 255)
            style = Paint.Style.FILL
        }

        private var scanLineY = 0f

        // Button rectangles
        private var startButtonRect = RectF()
        private var settingsButtonRect = RectF()

        init {
            isFocusable = true
            isClickable = true

            // Initialize rain
            for (layer in 0..2) {
                val dropCount = when(layer) {
                    0 -> 30
                    1 -> 50
                    else -> 80
                }

                for (i in 0 until dropCount) {
                    val speedMultiplier = when(layer) {
                        0 -> 0.5f
                        1 -> 1.0f
                        else -> 1.8f
                    }

                    rainDrops.add(
                        RainDrop(
                            x = Random.nextFloat() * 1080f,
                            y = Random.nextFloat() * 1920f,
                            speed = (400f + Random.nextFloat() * 600f) * speedMultiplier,
                            length = (20f + Random.nextFloat() * 40f) * speedMultiplier,
                            alpha = when(layer) {
                                0 -> Random.nextInt(30, 60)
                                1 -> Random.nextInt(60, 120)
                                else -> Random.nextInt(120, 200)
                            },
                            layer = layer
                        )
                    )
                }
            }
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)

            // Update background gradient
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

            // Position buttons
            val buttonWidth = w * 0.7f
            val buttonHeight = 120f
            val buttonCenterX = w / 2f

            startButtonRect = RectF(
                buttonCenterX - buttonWidth / 2f,
                h * 0.6f,
                buttonCenterX + buttonWidth / 2f,
                h * 0.6f + buttonHeight
            )

            settingsButtonRect = RectF(
                buttonCenterX - buttonWidth / 2f,
                h * 0.75f,
                buttonCenterX + buttonWidth / 2f,
                h * 0.75f + buttonHeight
            )
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val now = System.nanoTime()
            var dt = (now - lastTimeNs) / 1_000_000_000f
            if (dt > 0.05f) dt = 0.05f
            lastTimeNs = now
            elapsedTime += dt

            val w = width.toFloat()
            val h = height.toFloat()

            // Background
            canvas.drawRect(0f, 0f, w, h, bgPaint)

            // Rain animation
            updateRain(dt, w, h)
            drawRain(canvas)

            // Scan line
            scanLineY = (scanLineY + dt * 200f) % h
            canvas.drawRect(0f, scanLineY, w, scanLineY + 3f, scanLinePaint)

            // Title
            drawNeonText(canvas, w, h)

            // Buttons
            drawButtons(canvas)

            postInvalidateOnAnimation()
        }

        private fun updateRain(dt: Float, w: Float, h: Float) {
            for (drop in rainDrops) {
                drop.y += drop.speed * dt
                val drift = when(drop.layer) {
                    0 -> -20f * dt
                    1 -> -10f * dt
                    else -> -5f * dt
                }
                drop.x += drift

                if (drop.y > h + drop.length) {
                    drop.y = -drop.length
                    drop.x = Random.nextFloat() * w
                }
                if (drop.x < -10f) {
                    drop.x = w + 10f
                }
            }
        }

        private fun drawRain(canvas: Canvas) {
            for (layer in 0..2) {
                for (drop in rainDrops.filter { it.layer == layer }) {
                    rainPaint.color = Color.argb(drop.alpha, 100, 150, 255)
                    rainPaint.strokeWidth = if (drop.layer == 2) 3f else 2f
                    canvas.drawLine(drop.x, drop.y, drop.x, drop.y + drop.length, rainPaint)
                }
            }
        }

        private fun drawNeonText(canvas: Canvas, w: Float, h: Float) {
            val centerX = w / 2f
            val centerY = h * 0.3f

            // Flicker
            val flicker1 = 0.85f + sin(elapsedTime * 23.7f) * 0.05f +
                          sin(elapsedTime * 47.3f) * 0.05f +
                          (if (Random.nextFloat() < 0.05f) -0.15f else 0.05f)

            val alpha1 = (255 * flicker1).toInt().coerceIn(180, 255)

            neonGlowPaint.alpha = (alpha1 * 0.8f).toInt()
            neonCorePaint.alpha = alpha1
            neonOutlinePaint.alpha = alpha1

            // NEON
            canvas.drawText("NEON", centerX, centerY - 90f, neonGlowPaint)
            canvas.drawText("NEON", centerX, centerY - 90f, neonOutlinePaint)
            canvas.drawText("NEON", centerX, centerY - 90f, neonCorePaint)

            // SURVIVOR
            val flicker2 = 0.85f + sin(elapsedTime * 19.3f) * 0.05f +
                          sin(elapsedTime * 53.7f) * 0.05f +
                          (if (Random.nextFloat() < 0.05f) -0.15f else 0.05f)

            val alpha2 = (255 * flicker2).toInt().coerceIn(180, 255)

            neonGlowPaint.alpha = (alpha2 * 0.8f).toInt()
            neonCorePaint.alpha = alpha2
            neonOutlinePaint.alpha = alpha2

            canvas.drawText("SURVIVOR", centerX, centerY + 60f, neonGlowPaint)
            canvas.drawText("SURVIVOR", centerX, centerY + 60f, neonOutlinePaint)
            canvas.drawText("SURVIVOR", centerX, centerY + 60f, neonCorePaint)
        }

        private fun drawButtons(canvas: Canvas) {
            // START button
            canvas.drawRoundRect(startButtonRect, 20f, 20f, buttonBgPaint)
            canvas.drawRoundRect(startButtonRect, 20f, 20f, buttonBorderPaint)
            canvas.drawText(
                "START",
                startButtonRect.centerX(),
                startButtonRect.centerY() + 20f,
                buttonTextPaint
            )

            // SETTINGS button
            canvas.drawRoundRect(settingsButtonRect, 20f, 20f, buttonBgPaint)
            canvas.drawRoundRect(settingsButtonRect, 20f, 20f, buttonBorderPaint)
            canvas.drawText(
                "SETTINGS",
                settingsButtonRect.centerX(),
                settingsButtonRect.centerY() + 20f,
                buttonTextPaint
            )
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_UP) {
                val x = event.x
                val y = event.y

                if (startButtonRect.contains(x, y)) {
                    // Start game
                    val intent = Intent(context, MainActivity::class.java)
                    (context as Activity).startActivity(intent)
                    (context as Activity).finish()
                    return true
                }

                if (settingsButtonRect.contains(x, y)) {
                    // TODO: Settings screen (for now, just start game)
                    val intent = Intent(context, MainActivity::class.java)
                    (context as Activity).startActivity(intent)
                    (context as Activity).finish()
                    return true
                }
            }
            return true
        }
    }
