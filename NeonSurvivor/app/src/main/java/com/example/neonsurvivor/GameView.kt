package com.example.neonsurvivor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import kotlin.math.*
import kotlin.random.Random

class GameView(context: Context) : View(context) {

    // Timing
    private var lastTimeNs: Long = System.nanoTime()
    private var running: Boolean = true
    private var initialized: Boolean = false
    private var isPaused: Boolean = false
    private var countdownValue: Int = 0
    private var countdownAlpha: Float = 0f

    // Vibration
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    // Settings icon
    private var settingsIconRect = RectF()

    // Screen shake & damage feedback
    private var screenShakeX = 0f
    private var screenShakeY = 0f
    private var screenShakeTime = 0f
    private var damageFlashAlpha = 0f
    private var damageCooldown = 0f

    // Paints
    private val bgPaint = Paint().apply { color = Color.BLACK }
    private val gridPaint = Paint().apply {
        color = Color.argb(40, 0, 255, 0)
        strokeWidth = 1f
    }
    private val playerPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val enemyPaint = Paint().apply {
        color = Color.MAGENTA
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val bulletPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val bloodPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isAntiAlias = true
    }
    private val hpBgPaint = Paint().apply {
        color = Color.DKGRAY
        style = Paint.Style.FILL
    }
    private val hpFillPaint = Paint().apply {
        color = Color.rgb(0, 200, 100)
        style = Paint.Style.FILL
    }
    private val overlayPaint = Paint().apply {
        color = Color.argb(190, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val pauseOverlayPaint = Paint().apply {
        color = Color.argb(100, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val cardPaint = Paint().apply {
        color = Color.argb(220, 30, 30, 30)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val cardBorderPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val damageOverlayPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    // Player
    private var playerX = 0f
    private var playerY = 0f
    private var playerRadius = 30f
    private var playerSpeed = 250f  // Reduced from 300
    private var playerHp = 100
    private var maxHp = 100

    // Stats (rebalanced for difficulty)
    private var bulletDamage = 8f
    private var fireRate = 1.5f
    private var fireCooldown = 0f

    // Joystick
    private var joyBaseX = 0f
    private var joyBaseY = 0f
    private var joyPointerId = -1
    private var joyActive = false
    private var joyDx = 0f
    private var joyDy = 0f
    private val joyBaseRadius = 110f
    private val joyKnobRadius = 50f
    private val joyBasePaint = Paint().apply {
        color = Color.argb(80, 0, 255, 255)
        style = Paint.Style.FILL
    }
    private val joyKnobPaint = Paint().apply {
        color = Color.argb(160, 0, 255, 255)
        style = Paint.Style.FILL
    }

    data class Enemy(var x: Float, var y: Float, var radius: Float, var speed: Float, var hp: Float)
    data class Bullet(var x: Float, var y: Float, var vx: Float, var vy: Float)
    data class BloodParticle(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float)

    private val enemies = mutableListOf<Enemy>()
    private val bullets = mutableListOf<Bullet>()
    private val blood = mutableListOf<BloodParticle>()

    // Waves + gacha
    private var wave = 1
    private var inGacha = false

    enum class UpgradeType { DAMAGE, FIRE_RATE, SPEED, HP }
    data class UpgradeOption(val type: UpgradeType, val label: String, val desc: String)
    private val upgradeOptions = mutableListOf<UpgradeOption>()

    init {
        isFocusable = true
        isClickable = true
    }

    fun pause() {
        running = false
    }

    fun resume() {
        running = true
        if (initialized) {
            if (isPaused) {
                // Start 3-2-1 countdown on unpause
                isPaused = false
                countdownValue = 3
                countdownAlpha = 1f
            }
            lastTimeNs = System.nanoTime()
            postInvalidateOnAnimation()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!initialized && w > 0 && h > 0) {
            playerX = w / 2f
            playerY = h / 2f
            joyBaseX = w * 0.18f
            joyBaseY = h * 0.8f

            // Settings icon in top-right corner
            val iconSize = 60f
            settingsIconRect = RectF(
                w - iconSize - 20f,
                20f,
                w - 20f,
                20f + iconSize
            )

            spawnWave()

            // Start audio
            AudioManager.startMusic(context)
            AudioManager.startRain(context)

            lastTimeNs = System.nanoTime()
            running = true
            initialized = true
            postInvalidateOnAnimation()
        }
    }

    // --- Game loop via onDraw ---

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!initialized) {
            // no size yet; just draw black
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            return
        }

        val now = System.nanoTime()
        var dt = (now - lastTimeNs) / 1_000_000_000f
        if (dt > 0.05f) dt = 0.05f
        lastTimeNs = now

        if (running && !isPaused) {
            // Handle countdown
            if (countdownValue > 0) {
                countdownAlpha -= dt * 2f
                if (countdownAlpha <= 0f) {
                    countdownValue--
                    countdownAlpha = 1f
                }
            } else {
                update(dt)
            }
        }

        drawInternal(canvas)

        if (running) {
            postInvalidateOnAnimation()
        }
    }

    private fun spawnWave() {
        enemies.clear()
        // Better curve: starts at 5, slower growth for early waves
        val count = 5 + (wave * wave) / 2
        val rnd = Random(System.currentTimeMillis())
        for (i in 0 until count) {
            val edge = rnd.nextInt(4)
            val ex: Float
            val ey: Float
            when (edge) {
                0 -> { ex = rnd.nextFloat() * width; ey = -60f }
                1 -> { ex = rnd.nextFloat() * width; ey = height + 60f }
                2 -> { ex = -60f; ey = rnd.nextFloat() * height }
                else -> { ex = width + 60f; ey = rnd.nextFloat() * height }
            }
            // Faster and tankier: base speed 100 (was 60), scaling 15/wave (was 10)
            val baseSpeed = 100f + wave * 15f
            // HP scaling increased: 10/wave (was 5)
            val hp = 30f + wave * 10f
            enemies.add(Enemy(ex, ey, 24f, baseSpeed, hp))
        }
        if (playerHp <= 0) {
            playerHp = maxHp
        }
        inGacha = false
    }

    private fun update(dt: Float) {
        if (playerHp <= 0) {
            // Return to splash screen on death
            val intent = Intent(context, SplashActivity::class.java)
            (context as Activity).startActivity(intent)
            (context as Activity).finish()
            return
        }

        if (!inGacha) {
            updatePlayer(dt)
            updateEnemies(dt)
            updateBullets(dt)
            handleAutoFire(dt)
        }

        updateBlood(dt)
        updateScreenEffects(dt)

        if (!inGacha && enemies.isEmpty()) {
            openGacha()
        }
    }

    private fun updateScreenEffects(dt: Float) {
        // Update screen shake
        if (screenShakeTime > 0f) {
            screenShakeTime -= dt
            val intensity = 15f * (screenShakeTime / 0.3f) // Decay over time
            screenShakeX = (Random.nextFloat() - 0.5f) * 2f * intensity
            screenShakeY = (Random.nextFloat() - 0.5f) * 2f * intensity
        } else {
            screenShakeX = 0f
            screenShakeY = 0f
        }

        // Update damage flash
        if (damageFlashAlpha > 0f) {
            damageFlashAlpha -= dt * 300f // Fade out quickly
            if (damageFlashAlpha < 0f) damageFlashAlpha = 0f
        }
    }

    private fun updatePlayer(dt: Float) {
        if (joyActive) {
            val len = hypot(joyDx.toDouble(), joyDy.toDouble()).toFloat()
            if (len > 0.01f) {
                val nx = joyDx / len
                val ny = joyDy / len
                playerX += nx * playerSpeed * dt
                playerY += ny * playerSpeed * dt
            }
        }
        val w = width.toFloat()
        val h = height.toFloat()
        playerX = playerX.coerceIn(playerRadius, w - playerRadius)
        playerY = playerY.coerceIn(playerRadius, h - playerRadius)
    }

    private fun updateEnemies(dt: Float) {
        val it = enemies.iterator()
        var tookDamage = false
        while (it.hasNext()) {
            val e = it.next()
            val dx = playerX - e.x
            val dy = playerY - e.y
            val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (dist > 1f) {
                val nx = dx / dist
                val ny = dy / dist
                e.x += nx * e.speed * dt
                e.y += ny * e.speed * dt
            }
            if (dist < e.radius + playerRadius && damageCooldown <= 0f) {
                // Increased damage from 15 to 25 per second, but with cooldown
                playerHp -= 25
                if (playerHp < 0) playerHp = 0
                tookDamage = true
                damageCooldown = 0.5f // Half second between damage ticks
            }
        }

        if (tookDamage) {
            triggerDamageFeedback()
        }

        // Update damage cooldown
        if (damageCooldown > 0f) {
            damageCooldown -= dt
        }
    }

    private fun triggerDamageFeedback() {
        // Screen shake
        screenShakeTime = 0.3f

        // Damage flash
        damageFlashAlpha = 120f

        // Haptic feedback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    private fun updateBullets(dt: Float) {
        val bulletIt = bullets.iterator()
        while (bulletIt.hasNext()) {
            val b = bulletIt.next()
            b.x += b.vx * dt
            b.y += b.vy * dt

            val w = width.toFloat()
            val h = height.toFloat()
            if (b.x < -100 || b.x > w + 100 || b.y < -100 || b.y > h + 100) {
                bulletIt.remove()
                continue
            }

            var hitEnemy: Enemy? = null
            for (e in enemies) {
                val dx = e.x - b.x
                val dy = e.y - b.y
                val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                if (dist < e.radius) {
                    hitEnemy = e
                    break
                }
            }
            if (hitEnemy != null) {
                hitEnemy.hp -= bulletDamage
                spawnBlood(hitEnemy.x, hitEnemy.y)
                bulletIt.remove()
                if (hitEnemy.hp <= 0f) {
                    enemies.remove(hitEnemy)
                }
            }
        }
    }

    private fun handleAutoFire(dt: Float) {
        if (enemies.isEmpty()) return
        fireCooldown -= dt
        if (fireCooldown > 0f) return

        var target: Enemy? = null
        var bestDist2 = Float.MAX_VALUE
        for (e in enemies) {
            val dx = e.x - playerX
            val dy = e.y - playerY
            val d2 = dx * dx + dy * dy
            if (d2 < bestDist2) {
                bestDist2 = d2
                target = e
            }
        }

        target?.let {
            val dx = it.x - playerX
            val dy = it.y - playerY
            val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (dist > 1f) {
                val nx = dx / dist
                val ny = dy / dist
                val bulletSpeed = 700f
                bullets.add(Bullet(playerX, playerY, nx * bulletSpeed, ny * bulletSpeed))
                fireCooldown = 1f / fireRate
            }
        }
    }

    private fun spawnBlood(x: Float, y: Float) {
        val rnd = Random(System.nanoTime())
        for (i in 0 until 12) {
            val angle = rnd.nextFloat() * (2f * Math.PI.toFloat())
            val speed = 80f + rnd.nextFloat() * 120f
            val vx = cos(angle) * speed
            val vy = sin(angle) * speed
            blood.add(BloodParticle(x, y, vx, vy, 0.6f + rnd.nextFloat() * 0.4f))
        }
    }

    private fun updateBlood(dt: Float) {
        val it = blood.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.life -= dt
            if (p.life <= 0f) it.remove()
        }
    }

    private fun openGacha() {
        inGacha = true

        // Reset joystick state to prevent auto-run after unpause
        joyPointerId = -1
        joyActive = false
        joyDx = 0f
        joyDy = 0f

        upgradeOptions.clear()
        val allTypes = UpgradeType.values().toMutableList()
        allTypes.shuffle()
        val chosen = allTypes.take(3)
        for (t in chosen) {
            when (t) {
                UpgradeType.DAMAGE ->
                    upgradeOptions.add(
                        UpgradeOption(t, "Damage +20%", "Bullets hit harder.")
                    )
                UpgradeType.FIRE_RATE ->
                    upgradeOptions.add(
                        UpgradeOption(t, "Fire Rate +20%", "Shoot more often.")
                    )
                UpgradeType.SPEED ->
                    upgradeOptions.add(
                        UpgradeOption(t, "Speed +15%", "Move faster.")
                    )
                UpgradeType.HP ->
                    upgradeOptions.add(
                        UpgradeOption(t, "HP Boost", "Max HP +20 and heal.")
                    )
            }
        }
    }

    private fun applyUpgrade(option: UpgradeOption) {
        when (option.type) {
            UpgradeType.DAMAGE -> bulletDamage *= 1.2f  // Nerfed from 1.3
            UpgradeType.FIRE_RATE -> fireRate *= 1.2f  // Nerfed from 1.3
            UpgradeType.SPEED -> playerSpeed *= 1.15f  // Nerfed from 1.2
            UpgradeType.HP -> {
                maxHp += 20
                playerHp = maxHp
            }
        }
        wave += 1
        spawnWave()
        inGacha = false
    }

    private fun handleGachaTouch(x: Float, y: Float) {
        if (!inGacha) return
        val cardWidth = width * 0.8f
        val cardHeight = height * 0.12f
        val startX = (width - cardWidth) / 2f
        val firstY = height * 0.35f
        val gap = cardHeight + height * 0.03f

        for (i in upgradeOptions.indices) {
            val top = firstY + i * gap
            val rect = RectF(
                startX,
                top,
                startX + cardWidth,
                top + cardHeight
            )
            if (rect.contains(x, y)) {
                applyUpgrade(upgradeOptions[i])
                return
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val index = event.actionIndex

        // Check settings icon tap (not paused or in gacha)
        if (action == MotionEvent.ACTION_UP && !inGacha) {
            val x = event.getX(index)
            val y = event.getY(index)
            if (settingsIconRect.contains(x, y)) {
                isPaused = true
                val intent = Intent(context, SettingsActivity::class.java)
                (context as Activity).startActivity(intent)
                // Will unpause in onResume
                return true
            }
        }

        if (inGacha && (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP)) {
            val x = event.getX(index)
            val y = event.getY(index)
            handleGachaTouch(x, y)
            return true
        }

        when (action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                val x = event.getX(index)
                val y = event.getY(index)
                if (x < width * 0.5f && y > height * 0.5f && joyPointerId == -1) {
                    joyPointerId = event.getPointerId(index)
                    joyBaseX = x
                    joyBaseY = y
                    joyDx = 0f
                    joyDy = 0f
                    joyActive = true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (joyPointerId != -1) {
                    val pIndex = event.findPointerIndex(joyPointerId)
                    if (pIndex != -1) {
                        val x = event.getX(pIndex)
                        val y = event.getY(pIndex)
                        joyDx = x - joyBaseX
                        joyDy = y - joyBaseY
                        val len = hypot(joyDx.toDouble(), joyDy.toDouble()).toFloat()
                        if (len > joyBaseRadius) {
                            val scale = joyBaseRadius / len
                            joyDx *= scale
                            joyDy *= scale
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_CANCEL -> {
                val pid = event.getPointerId(index)
                if (pid == joyPointerId) {
                    joyPointerId = -1
                    joyActive = false
                    joyDx = 0f
                    joyDy = 0f
                }
            }
        }
        return true
    }

    private fun drawInternal(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        // Apply screen shake by translating canvas
        canvas.save()
        canvas.translate(screenShakeX, screenShakeY)

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        var x = 0f
        while (x < w) {
            canvas.drawLine(x, 0f, x, h, gridPaint)
            x += 80f
        }

        for (p in blood) {
            val alpha = (255f * p.life.coerceIn(0f, 1f)).toInt()
            bloodPaint.alpha = alpha
            canvas.drawCircle(p.x, p.y, 6f, bloodPaint)
        }

        for (b in bullets) {
            canvas.drawCircle(b.x, b.y, 6f, bulletPaint)
        }

        for (e in enemies) {
            canvas.drawCircle(e.x, e.y, e.radius, enemyPaint)
        }

        canvas.drawCircle(playerX, playerY, playerRadius, playerPaint)

        val barWidth = w * 0.6f
        val barHeight = 24f
        val barX = (w - barWidth) / 2f
        val barY = 40f
        canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, hpBgPaint)
        val hpRatio = (playerHp.toFloat() / maxHp.toFloat()).coerceIn(0f, 1f)
        canvas.drawRect(barX, barY, barX + barWidth * hpRatio, barY + barHeight, hpFillPaint)
        canvas.drawText("Wave $wave", 20f, barY + barHeight + 40f, textPaint)

        // Pause icon with semi-transparent background
        val pauseBgPaint = Paint().apply {
            color = Color.argb(80, 0, 0, 0)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val pauseIconPaint = Paint().apply {
            color = Color.CYAN
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Draw semi-transparent background circle
        canvas.drawCircle(settingsIconRect.centerX(), settingsIconRect.centerY(), 30f, pauseBgPaint)

        // Draw pause bars (two vertical rectangles)
        val pauseCenterX = settingsIconRect.centerX()
        val pauseCenterY = settingsIconRect.centerY()
        val pauseBarWidth = 8f
        val pauseBarHeight = 26f
        val pauseBarGap = 10f

        canvas.drawRoundRect(
            pauseCenterX - pauseBarGap - pauseBarWidth,
            pauseCenterY - pauseBarHeight / 2,
            pauseCenterX - pauseBarGap,
            pauseCenterY + pauseBarHeight / 2,
            4f, 4f,
            pauseIconPaint
        )
        canvas.drawRoundRect(
            pauseCenterX + pauseBarGap,
            pauseCenterY - pauseBarHeight / 2,
            pauseCenterX + pauseBarGap + pauseBarWidth,
            pauseCenterY + pauseBarHeight / 2,
            4f, 4f,
            pauseIconPaint
        )

        if (joyActive) {
            canvas.drawCircle(joyBaseX, joyBaseY, joyBaseRadius, joyBasePaint)
            val knobX = joyBaseX + joyDx
            val knobY = joyBaseY + joyDy
            canvas.drawCircle(knobX, knobY, joyKnobRadius, joyKnobPaint)
        } else {
            canvas.drawCircle(joyBaseX, joyBaseY, joyBaseRadius * 0.5f, joyBasePaint)
        }

        if (inGacha) {
            canvas.drawRect(0f, 0f, w, h, overlayPaint)
            canvas.drawText("Choose an upgrade:", w * 0.1f, h * 0.25f, textPaint)

            val cardWidth = w * 0.8f
            val cardHeight = h * 0.12f
            val startX = (w - cardWidth) / 2f
            val firstY = h * 0.35f
            val gap = cardHeight + h * 0.03f

            upgradeOptions.forEachIndexed { i, opt ->
                val top = firstY + i * gap
                val rect = RectF(
                    startX,
                    top,
                    startX + cardWidth,
                    top + cardHeight
                )
                canvas.drawRoundRect(rect, 16f, 16f, cardPaint)
                canvas.drawRoundRect(rect, 16f, 16f, cardBorderPaint)
                canvas.drawText(opt.label, rect.left + 40f, rect.top + 60f, textPaint)
                canvas.drawText(opt.desc, rect.left + 40f, rect.top + 110f, textPaint)
            }
        }

        // Restore canvas from screen shake
        canvas.restore()

        // Draw damage flash overlay AFTER restoring canvas (so it covers everything without shake)
        if (damageFlashAlpha > 0f) {
            damageOverlayPaint.alpha = damageFlashAlpha.toInt()
            canvas.drawRect(0f, 0f, w, h, damageOverlayPaint)
        }

        // Draw countdown
        if (countdownValue > 0) {
            val countdownPaint = Paint().apply {
                color = Color.CYAN
                textSize = 200f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
                alpha = (255 * countdownAlpha).toInt()
            }
            canvas.drawText(countdownValue.toString(), w / 2f, h / 2f + 70f, countdownPaint)
        }
    }
}
