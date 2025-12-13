package com.example.neonsurvivor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

class CreditsActivity : Activity() {

    private lateinit var creditsView: CreditsView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        creditsView = CreditsView(this)
        setContentView(creditsView)
    }
}

class CreditsView(context: Context) : View(context) {

    private val activity = context as Activity

    // Paints
    private val bgPaint = Paint()
    private val titlePaint = Paint().apply {
        color = Color.CYAN
        textSize = 80f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isAntiAlias = true
    }

    private val sectionPaint = Paint().apply {
        color = Color.YELLOW
        textSize = 50f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 35f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        isAntiAlias = true
    }

    private val linkPaint = Paint().apply {
        color = Color.argb(255, 100, 200, 255)  // Light blue for links
        textSize = 30f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
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

    private val buttonTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 50f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isAntiAlias = true
    }

    // UI elements
    private var backButtonRect = RectF()

    // Clickable link areas
    private val linkRects = mutableMapOf<String, RectF>()

    init {
        isFocusable = true
        isClickable = true
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        // Background gradient
        val bgShader = LinearGradient(0f, 0f, 0f, h, Color.BLACK, Color.argb(255, 20, 0, 40), Shader.TileMode.CLAMP)
        bgPaint.shader = bgShader
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        var yPos = h * 0.1f

        // Title
        canvas.drawText("CREDITS", w / 2f, yPos, titlePaint)
        yPos += 80f

        // Art Section
        canvas.drawText("ART & SPRITES", w / 2f, yPos, sectionPaint)
        yPos += 60f

        canvas.drawText("Cyberpunk Character Sprites", w / 2f, yPos, textPaint)
        yPos += 40f
        drawClickableLink(canvas, "free-game-assets.itch.io",
            "https://free-game-assets.itch.io/free-3-cyberpunk-sprites-pixel-art",
            w / 2f, yPos)
        yPos += 50f

        canvas.drawText("16x16 RPG Character Sprites", w / 2f, yPos, textPaint)
        yPos += 40f
        drawClickableLink(canvas, "route1rodent.itch.io",
            "https://route1rodent.itch.io/16x16-rpg-character-sprite-sheet",
            w / 2f, yPos)
        yPos += 80f

        // Music Section
        canvas.drawText("MUSIC & SOUND", w / 2f, yPos, sectionPaint)
        yPos += 60f

        canvas.drawText("Pink Bloom Synthwave Music Pack", w / 2f, yPos, textPaint)
        yPos += 40f
        drawClickableLink(canvas, "davidkbd.itch.io",
            "https://davidkbd.itch.io/pink-bloom-synthwave-music-pack",
            w / 2f, yPos)
        yPos += 80f

        // Development Section
        canvas.drawText("DEVELOPMENT", w / 2f, yPos, sectionPaint)
        yPos += 60f

        canvas.drawText("Game Design & Programming", w / 2f, yPos, textPaint)
        yPos += 40f
        canvas.drawText("timmseth", w / 2f, yPos, textPaint)
        yPos += 50f

        canvas.drawText("With assistance from Claude Sonnet 4.5", w / 2f, yPos, textPaint)
        yPos += 40f
        drawClickableLink(canvas, "claude.com/claude-code",
            "https://claude.com/claude-code",
            w / 2f, yPos)
        yPos += 80f

        // Support Section
        canvas.drawText("SUPPORT", w / 2f, yPos, sectionPaint)
        yPos += 60f
        drawClickableLink(canvas, "Support on Ko-fi: timmseth",
            "https://ko-fi.com/timmseth",
            w / 2f, yPos)

        // Back button at bottom
        val buttonWidth = 300f
        val buttonHeight = 80f
        backButtonRect = RectF(
            w / 2f - buttonWidth / 2f,
            h - 120f,
            w / 2f + buttonWidth / 2f,
            h - 40f
        )
        canvas.drawRoundRect(backButtonRect, 20f, 20f, buttonBgPaint)
        canvas.drawRoundRect(backButtonRect, 20f, 20f, buttonBorderPaint)
        canvas.drawText("BACK", backButtonRect.centerX(), backButtonRect.centerY() + 20f, buttonTextPaint)
    }

    private fun drawClickableLink(canvas: Canvas, text: String, url: String, x: Float, y: Float) {
        canvas.drawText(text, x, y, linkPaint)

        // Store clickable area (approximate)
        val textWidth = linkPaint.measureText(text)
        val rect = RectF(
            x - textWidth / 2f - 10f,
            y - 35f,
            x + textWidth / 2f + 10f,
            y + 10f
        )
        linkRects[url] = rect
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x
            val y = event.y

            // Check back button
            if (backButtonRect.contains(x, y)) {
                activity.finish()
                return true
            }

            // Check link clicks
            for ((url, rect) in linkRects) {
                if (rect.contains(x, y)) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // If can't open browser, just ignore
                    }
                    return true
                }
            }
        }
        return true
    }
}
