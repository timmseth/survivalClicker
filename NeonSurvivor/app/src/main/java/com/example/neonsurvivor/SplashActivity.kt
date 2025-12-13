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

        // Kill counter paint (flickering neon magenta)
        private val killCounterPaint = Paint().apply {
            color = Color.MAGENTA
            textSize = 80f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(30f, 0f, 0f, Color.MAGENTA)
        }

        // Load high score from prefs
        private val prefs = context.getSharedPreferences("game_stats", Context.MODE_PRIVATE)

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
        private var creditsButtonRect = RectF()

        // Character selection
        private var selectedCharacter = 0  // 0=Biker, 1=Punk, 2=Cyborg
        private val characterNames = listOf("BIKER", "PUNK", "CYBORG")
        private var characterIdleSprites = mutableListOf<Bitmap>()
        private var characterRunSprites = mutableListOf<Bitmap>()
        private var characterAnimFrame = 0
        private var characterAnimTime = 0f
        private var isRunning = false  // Alternate between idle and run
        private var runToggleTime = 0f
        private var leftArrowRect = RectF()
        private var rightArrowRect = RectF()
        private var characterSpotlightRect = RectF()

        private val arrowPaint = Paint().apply {
            color = Color.CYAN
            textSize = 120f  // Increased from 80f for better visibility
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(30f, 0f, 0f, Color.CYAN)  // Increased glow
        }

        private val characterNamePaint = Paint().apply {
            color = Color.YELLOW
            textSize = 40f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(15f, 0f, 0f, Color.YELLOW)
        }

        private val spotlightPaint = Paint().apply {
            color = Color.argb(100, 0, 255, 255)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        private val kofiPaint = Paint().apply {
            color = Color.argb(180, 100, 100, 100)  // Subtle gray
            textSize = 24f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        init {
            isFocusable = true
            isClickable = true

            // Load character sprites
            characterIdleSprites.add(BitmapFactory.decodeResource(context.resources, R.drawable.char_biker_idle))
            characterIdleSprites.add(BitmapFactory.decodeResource(context.resources, R.drawable.char_punk_idle))
            characterIdleSprites.add(BitmapFactory.decodeResource(context.resources, R.drawable.char_cyborg_idle))

            characterRunSprites.add(BitmapFactory.decodeResource(context.resources, R.drawable.char_biker_run))
            characterRunSprites.add(BitmapFactory.decodeResource(context.resources, R.drawable.char_punk_run))
            characterRunSprites.add(BitmapFactory.decodeResource(context.resources, R.drawable.char_cyborg_run))

            // Load saved character selection
            selectedCharacter = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getInt("selected_character", 0)

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

            creditsButtonRect = RectF(
                buttonCenterX - buttonWidth / 2f,
                h * 0.9f,
                buttonCenterX + buttonWidth / 2f,
                h * 0.9f + buttonHeight
            )

            // Position character spotlight (between SURVIVOR text and START button)
            val spotlightSize = 280f  // Increased from 200f for better visibility
            characterSpotlightRect = RectF(
                buttonCenterX - spotlightSize / 2f,
                h * 0.45f,
                buttonCenterX + spotlightSize / 2f,
                h * 0.45f + spotlightSize
            )

            // Position arrow buttons
            val arrowSize = 120f  // Increased from 80f for easier tapping
            leftArrowRect = RectF(
                characterSpotlightRect.left - arrowSize - 20f,
                characterSpotlightRect.centerY() - arrowSize / 2f,
                characterSpotlightRect.left - 20f,
                characterSpotlightRect.centerY() + arrowSize / 2f
            )

            rightArrowRect = RectF(
                characterSpotlightRect.right + 20f,
                characterSpotlightRect.centerY() - arrowSize / 2f,
                characterSpotlightRect.right + arrowSize + 20f,
                characterSpotlightRect.centerY() + arrowSize / 2f
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

            // Character selection
            updateCharacterAnimation(dt)
            drawCharacterSelection(canvas)

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

            // Draw kill counter with flickering effect
            val highScore = prefs.getInt("high_score", 0)
            if (highScore > 0) {
                // Flickering neon effect (broken motel sign style)
                val flicker = 0.85f + sin(elapsedTime * 8.2f) * 0.08f +
                              sin(elapsedTime * 17.3f) * 0.04f +
                              (if (Random.nextFloat() < 0.1f) -0.3f else 0.03f)

                // Hue shift effect (cycle through pink/magenta/purple)
                val hueShift = (elapsedTime * 40f) % 360f
                val hue = (300f + hueShift % 60f) // Cycle between 300-360 (pink/magenta range)
                val color = Color.HSVToColor(floatArrayOf(hue, 0.8f, 1.0f))

                killCounterPaint.color = color
                killCounterPaint.alpha = (255 * flicker).toInt().coerceIn(150, 255)
                killCounterPaint.setShadowLayer(40f, 0f, 0f, color)

                canvas.drawText("BEST: $highScore KILLS", centerX, centerY + 180f, killCounterPaint)
            }
        }

        private fun updateCharacterAnimation(dt: Float) {
            // Toggle between idle and run every 3 seconds
            runToggleTime += dt
            if (runToggleTime >= 3f) {
                runToggleTime = 0f
                isRunning = !isRunning
                characterAnimFrame = 0
            }

            // Update animation frame
            characterAnimTime += dt
            val frameDelay = 0.15f  // ~6.7 FPS animation
            if (characterAnimTime >= frameDelay) {
                characterAnimTime = 0f
                val maxFrames = if (isRunning) 6 else 4
                characterAnimFrame = (characterAnimFrame + 1) % maxFrames
            }
        }

        private fun drawCharacterSelection(canvas: Canvas) {
            // Draw spotlight background
            canvas.drawOval(characterSpotlightRect, spotlightPaint)

            // Draw character sprite
            val spriteSheet = if (isRunning) {
                characterRunSprites[selectedCharacter]
            } else {
                characterIdleSprites[selectedCharacter]
            }

            val totalFrames = if (isRunning) 6 else 4
            val frameWidth = spriteSheet.width / totalFrames
            val frameHeight = spriteSheet.height

            val srcRect = Rect(
                characterAnimFrame * frameWidth,
                0,
                (characterAnimFrame + 1) * frameWidth,
                frameHeight
            )

            // Apply same centering offset as in-game (sprite content is bottom-left of frame)
            val offsetX = 20f  // Shift right
            val offsetY = -20f  // Shift up

            val adjustedRect = RectF(
                characterSpotlightRect.left + offsetX,
                characterSpotlightRect.top + offsetY,
                characterSpotlightRect.right + offsetX,
                characterSpotlightRect.bottom + offsetY
            )

            canvas.drawBitmap(spriteSheet, srcRect, adjustedRect, null)

            // Draw character name
            canvas.drawText(
                characterNames[selectedCharacter],
                characterSpotlightRect.centerX(),
                characterSpotlightRect.bottom + 50f,
                characterNamePaint
            )

            // Draw arrows (larger and more visible)
            canvas.drawText("<", leftArrowRect.centerX(), leftArrowRect.centerY() + 35f, arrowPaint)
            canvas.drawText(">", rightArrowRect.centerX(), rightArrowRect.centerY() + 35f, arrowPaint)
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

            // CREDITS button
            canvas.drawRoundRect(creditsButtonRect, 20f, 20f, buttonBgPaint)
            canvas.drawRoundRect(creditsButtonRect, 20f, 20f, buttonBorderPaint)
            canvas.drawText(
                "CREDITS",
                creditsButtonRect.centerX(),
                creditsButtonRect.centerY() + 20f,
                buttonTextPaint
            )

            // Ko-fi link at bottom
            val w = width.toFloat()
            val h = height.toFloat()
            canvas.drawText(
                "Support on Ko-fi: timmseth",
                w / 2f,
                h - 40f,
                kofiPaint
            )
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_UP) {
                val x = event.x
                val y = event.y

                // Left arrow - previous character
                if (leftArrowRect.contains(x, y)) {
                    selectedCharacter = (selectedCharacter - 1 + 3) % 3
                    characterAnimFrame = 0
                    // Save selection
                    context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                        .edit()
                        .putInt("selected_character", selectedCharacter)
                        .apply()
                    return true
                }

                // Right arrow - next character
                if (rightArrowRect.contains(x, y)) {
                    selectedCharacter = (selectedCharacter + 1) % 3
                    characterAnimFrame = 0
                    // Save selection
                    context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                        .edit()
                        .putInt("selected_character", selectedCharacter)
                        .apply()
                    return true
                }

                if (startButtonRect.contains(x, y)) {
                    // Start game
                    val intent = Intent(context, MainActivity::class.java)
                    (context as Activity).startActivity(intent)
                    (context as Activity).finish()
                    return true
                }

                if (settingsButtonRect.contains(x, y)) {
                    // Open settings
                    val intent = Intent(context, SettingsActivity::class.java)
                    (context as Activity).startActivity(intent)
                    return true
                }

                if (creditsButtonRect.contains(x, y)) {
                    // Open credits
                    val intent = Intent(context, CreditsActivity::class.java)
                    (context as Activity).startActivity(intent)
                    return true
                }
            }
            return true
        }
    }
