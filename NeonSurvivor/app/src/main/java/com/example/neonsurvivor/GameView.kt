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

    // Camera offset for centering player
    private var cameraX = 0f
    private var cameraY = 0f

    // Settings icon
    private var settingsIconRect = RectF()

    // Screen shake & damage feedback
    private var screenShakeX = 0f
    private var screenShakeY = 0f
    private var screenShakeTime = 0f
    private var damageFlashAlpha = 0f
    private var damageCooldown = 0f

    // Paints
    private val bgPaint = Paint().apply { color = Color.rgb(5, 5, 15) } // Darker cyberpunk bg
    private val gridPaint = Paint().apply {
        color = Color.argb(60, 0, 255, 255) // Brighter cyan grid
        strokeWidth = 2f
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
    private val enemyGlowPaint = Paint().apply {
        color = Color.MAGENTA
        style = Paint.Style.FILL
        isAntiAlias = true
        maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL) // Magenta glow
    }
    private val bulletPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.FILL
        strokeWidth = 6f
        isAntiAlias = true
    }
    private val bulletGlowPaint = Paint().apply {
        color = Color.CYAN
        isAntiAlias = true
        maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL) // Cyan bullet glow
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

    // Player sprite animation
    private val playerIdleSprite: Bitmap
    private val playerRunSprite: Bitmap
    private val playerHitSprite: Bitmap
    private val playerDeathSprite: Bitmap
    private var spriteFrameTime = 0f
    private var currentFrame = 0
    private val frameDelay = 0.1f // 10 FPS animation
    private var hitAnimationTime = 0f
    private var isPlayingHitAnimation = false
    private var isDying = false
    private var deathAnimationTime = 0f
    private val spriteWidth = 576f // 3x wider (3 * 192)
    private val spriteHeight = 192f // 3x larger height (was 64f)
    private val spritePaint = Paint().apply {
        isAntiAlias = false // Pixel art should not be antialiased
        isFilterBitmap = false // Keep crisp pixels
    }
    private val spriteGlowPaint = Paint().apply {
        color = Color.CYAN
        isAntiAlias = true
        maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL) // Reduced glow (was 30f)
    }

    // Player
    private var playerX = 0f
    private var playerY = 0f
    private var playerRadius = 96f // 3x larger hitbox (was 32f)
    private var playerSpeed = 250f  // Reduced from 300
    private var playerHp = 100
    private var maxHp = 100

    // Stats (rebalanced for difficulty)
    private var bulletDamage = 8f
    private var fireRate = 1.5f
    private var fireCooldown = 0f

    // Kill tracking
    private var killCount = 0
    private val prefs = context.getSharedPreferences("game_stats", Context.MODE_PRIVATE)

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

    // Death screen
    private var inDeathScreen = false
    private var deathScreenFadeAlpha = 0f
    private var rebornButtonRect = RectF()
    private var dieButtonRect = RectF()

    enum class UpgradeType { DAMAGE, FIRE_RATE, SPEED, HP }
    data class UpgradeOption(val type: UpgradeType, val label: String, val desc: String)
    private val upgradeOptions = mutableListOf<UpgradeOption>()

    init {
        isFocusable = true
        isClickable = true

        // Enable software rendering for blur effects (BlurMaskFilter doesn't work with hardware acceleration)
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        // Load player sprites
        playerIdleSprite = BitmapFactory.decodeResource(resources, R.drawable.player_idle)
        playerRunSprite = BitmapFactory.decodeResource(resources, R.drawable.player_run)
        playerHitSprite = BitmapFactory.decodeResource(resources, R.drawable.player_attack) // Using attack as hit
        playerDeathSprite = BitmapFactory.decodeResource(resources, R.drawable.player_death)
    }

    fun pause() {
        running = false
    }

    fun resume() {
        running = true
        if (initialized) {
            // Check for save/load triggers
            if (prefs.getBoolean("trigger_save", false)) {
                saveGameState()
                prefs.edit().putBoolean("trigger_save", false).apply()
            }
            if (prefs.getBoolean("trigger_load", false)) {
                loadGameState()
                prefs.edit().putBoolean("trigger_load", false).apply()
            }

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
            // Floating joystick - no fixed position, appears where user touches

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
        if (playerHp <= 0 && !isDying && !inDeathScreen) {
            // Trigger death sequence
            isDying = true
            deathAnimationTime = 0f
            currentFrame = 0

            // Save high score if this run beat it
            val currentHighScore = prefs.getInt("high_score", 0)
            if (killCount > currentHighScore) {
                prefs.edit().putInt("high_score", killCount).apply()
            }
        }

        // Update death animation and transition to death screen
        if (isDying) {
            deathAnimationTime += dt
            deathScreenFadeAlpha = min(deathScreenFadeAlpha + dt * 200f, 255f)

            if (deathAnimationTime >= 1.5f) { // Death animation + delay
                isDying = false
                inDeathScreen = true
            }
            return // Don't update game while dying
        }

        if (inDeathScreen) {
            return // Freeze game on death screen
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
        var isMoving = false
        if (joyActive) {
            val len = hypot(joyDx.toDouble(), joyDy.toDouble()).toFloat()
            if (len > 0.01f) {
                val nx = joyDx / len
                val ny = joyDy / len
                playerX += nx * playerSpeed * dt
                playerY += ny * playerSpeed * dt
                isMoving = true
            }
        }

        // Update sprite animation
        if (isMoving) {
            spriteFrameTime += dt
            if (spriteFrameTime >= frameDelay) {
                spriteFrameTime = 0f
                // Run sprite has 8 frames
                currentFrame = (currentFrame + 1) % 8
            }
        } else {
            spriteFrameTime += dt
            if (spriteFrameTime >= frameDelay) {
                spriteFrameTime = 0f
                // Idle sprite has 5 frames
                currentFrame = (currentFrame + 1) % 5
            }
        }

        // Update camera to center on player
        val w = width.toFloat()
        val h = height.toFloat()
        cameraX = playerX - w / 2f
        cameraY = playerY - h / 2f
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

                // Trigger hit animation
                isPlayingHitAnimation = true
                hitAnimationTime = 0f
                currentFrame = 0
            }
        }

        if (tookDamage) {
            triggerDamageFeedback()
        }

        // Update damage cooldown
        if (damageCooldown > 0f) {
            damageCooldown -= dt
        }

        // Update hit animation
        if (isPlayingHitAnimation) {
            hitAnimationTime += dt
            if (hitAnimationTime >= 0.4f) { // Hit animation lasts 0.4 seconds
                isPlayingHitAnimation = false
                hitAnimationTime = 0f
            }
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

            // Remove bullets that are far from player (world coordinates, not screen coordinates)
            val dx = b.x - playerX
            val dy = b.y - playerY
            val distFromPlayer = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (distFromPlayer > 2000f) {
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
                    killCount++ // Increment kill counter
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

    private fun saveGameState() {
        prefs.edit().apply {
            putInt("saved_wave", wave)
            putInt("saved_hp", playerHp)
            putInt("saved_max_hp", maxHp)
            putFloat("saved_damage", bulletDamage)
            putFloat("saved_fire_rate", fireRate)
            putFloat("saved_speed", playerSpeed)
            putInt("saved_kills", killCount)
            apply()
        }
    }

    private fun loadGameState() {
        val savedWave = prefs.getInt("saved_wave", 0)
        if (savedWave > 0) {
            wave = savedWave
            playerHp = prefs.getInt("saved_hp", 100)
            maxHp = prefs.getInt("saved_max_hp", 100)
            bulletDamage = prefs.getFloat("saved_damage", 8f)
            fireRate = prefs.getFloat("saved_fire_rate", 1.5f)
            playerSpeed = prefs.getFloat("saved_speed", 250f)
            killCount = prefs.getInt("saved_kills", 0)

            // Spawn enemies for current wave
            enemies.clear()
            val count = 8 + wave * 3
            for (i in 0 until count) {
                val angle = Random.nextFloat() * 2 * PI.toFloat()
                val distance = 600f + Random.nextFloat() * 200f
                val ex = playerX + cos(angle) * distance
                val ey = playerY + sin(angle) * distance
                val baseSpeed = 100f + wave * 15f
                val hp = 30f + wave * 10f
                enemies.add(Enemy(ex, ey, 24f, baseSpeed, hp))
            }
        }
    }

    private fun clearSavedGame() {
        prefs.edit().apply {
            remove("saved_wave")
            remove("saved_hp")
            remove("saved_max_hp")
            remove("saved_damage")
            remove("saved_fire_rate")
            remove("saved_speed")
            remove("saved_kills")
            apply()
        }
    }

    private fun resetGame() {
        // Reset player state
        playerHp = maxHp
        killCount = 0
        wave = 1

        // Reset position
        playerX = width / 2f
        playerY = height / 2f
        cameraX = 0f
        cameraY = 0f

        // Clear entities
        enemies.clear()
        bullets.clear()
        blood.clear()

        // Reset death screen
        inDeathScreen = false
        isDying = false
        deathScreenFadeAlpha = 0f
        deathAnimationTime = 0f

        // Start first wave
        enemies.clear()
        val count = 8 + wave * 3
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2 * PI.toFloat()
            val distance = 600f + Random.nextFloat() * 200f
            val ex = playerX + cos(angle) * distance
            val ey = playerY + sin(angle) * distance
            val baseSpeed = 100f + wave * 15f
            val hp = 30f + wave * 10f
            enemies.add(Enemy(ex, ey, 24f, baseSpeed, hp))
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

        // Handle death screen button clicks
        if (inDeathScreen && action == MotionEvent.ACTION_UP) {
            val x = event.getX(index)
            val y = event.getY(index)

            when {
                rebornButtonRect.contains(x, y) -> {
                    // Reset game state for new run
                    resetGame()
                    return true
                }
                dieButtonRect.contains(x, y) -> {
                    // Return to splash screen
                    val intent = Intent(context, SplashActivity::class.java)
                    (context as Activity).startActivity(intent)
                    (context as Activity).finish()
                    return true
                }
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
                // Floating joystick: appears anywhere on screen
                if (joyPointerId == -1) {
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

        // Apply camera offset and screen shake by translating canvas
        canvas.save()
        canvas.translate(-cameraX + screenShakeX, -cameraY + screenShakeY)

        // Draw large background centered on player (much bigger than screen)
        val bgSize = 10000f // Large background for infinite world feel
        canvas.drawRect(
            playerX - bgSize, playerY - bgSize,
            playerX + bgSize, playerY + bgSize,
            bgPaint
        )

        // Draw grid with camera offset
        var gridStartX = (cameraX / 80f).toInt() * 80f
        var x = gridStartX
        while (x < cameraX + w) {
            canvas.drawLine(x, cameraY, x, cameraY + h, gridPaint)
            x += 80f
        }

        // Draw vertical grid lines too
        var gridStartY = (cameraY / 80f).toInt() * 80f
        var y = gridStartY
        while (y < cameraY + h) {
            canvas.drawLine(cameraX, y, cameraX + w, y, gridPaint)
            y += 80f
        }

        for (p in blood) {
            val alpha = (255f * p.life.coerceIn(0f, 1f)).toInt()
            bloodPaint.alpha = alpha
            canvas.drawCircle(p.x, p.y, 6f, bloodPaint)
        }

        for (b in bullets) {
            // Neon glow effect
            canvas.drawCircle(b.x, b.y, 12f, bulletGlowPaint)
            canvas.drawCircle(b.x, b.y, 6f, bulletPaint)
        }

        for (e in enemies) {
            // Neon glow effect
            canvas.drawCircle(e.x, e.y, e.radius + 10f, enemyGlowPaint)
            canvas.drawCircle(e.x, e.y, e.radius, enemyPaint)
        }

        // Draw player sprite
        val isMoving = joyActive && hypot(joyDx.toDouble(), joyDy.toDouble()).toFloat() > 0.01f

        // Choose sprite based on state (death > hit > movement)
        val spriteSheet: Bitmap
        val totalFrames: Int
        when {
            isDying -> {
                spriteSheet = playerDeathSprite
                totalFrames = 4 // Death animation frames
            }
            isPlayingHitAnimation -> {
                spriteSheet = playerHitSprite
                totalFrames = 4 // Hit animation frames
            }
            isMoving -> {
                spriteSheet = playerRunSprite
                totalFrames = 8
            }
            else -> {
                spriteSheet = playerIdleSprite
                totalFrames = 5
            }
        }

        // Calculate frame size (sprites are vertical strip)
        val frameWidth = spriteSheet.width
        val frameHeight = spriteSheet.height / totalFrames

        // Source rect for current frame
        val srcRect = Rect(
            0,
            currentFrame * frameHeight,
            frameWidth,
            (currentFrame + 1) * frameHeight
        )

        // Destination rect (where to draw on screen) - now 3x wider
        val dstRect = RectF(
            playerX - spriteWidth / 2f,
            playerY - spriteHeight / 2f,
            playerX + spriteWidth / 2f,
            playerY + spriteHeight / 2f
        )

        // Draw neon glow behind sprite (smaller radius)
        canvas.drawCircle(playerX, playerY, spriteHeight / 2.5f, spriteGlowPaint)

        canvas.drawBitmap(spriteSheet, srcRect, dstRect, spritePaint)

        // Restore canvas from camera and screen shake
        canvas.restore()

        // Draw UI elements in screen space (not world space)

        // HP bar
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

        // Joystick in screen space
        if (joyActive) {
            canvas.drawCircle(joyBaseX, joyBaseY, joyBaseRadius, joyBasePaint)
            val knobX = joyBaseX + joyDx
            val knobY = joyBaseY + joyDy
            canvas.drawCircle(knobX, knobY, joyKnobRadius, joyKnobPaint)
        }

        // Gacha menu overlay
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

        // Death screen overlay
        if (inDeathScreen || isDying) {
            // Fade to black (everything except player)
            val deathOverlayPaint = Paint().apply {
                color = Color.BLACK
                alpha = deathScreenFadeAlpha.toInt().coerceIn(0, 255)
            }
            canvas.drawRect(0f, 0f, w, h, deathOverlayPaint)

            if (inDeathScreen) {
                // Kill count in big neon
                val killCountPaint = Paint().apply {
                    color = Color.MAGENTA
                    textSize = 100f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    isAntiAlias = true
                    setShadowLayer(40f, 0f, 0f, Color.MAGENTA)
                }
                canvas.drawText("$killCount KILLS", w / 2f, h * 0.3f, killCountPaint)

                // Button dimensions
                val buttonWidth = w * 0.7f
                val buttonHeight = 120f
                val buttonCenterX = w / 2f

                // Reborn button (top)
                rebornButtonRect = RectF(
                    buttonCenterX - buttonWidth / 2f,
                    h * 0.5f,
                    buttonCenterX + buttonWidth / 2f,
                    h * 0.5f + buttonHeight
                )

                // Die button (bottom)
                dieButtonRect = RectF(
                    buttonCenterX - buttonWidth / 2f,
                    h * 0.65f,
                    buttonCenterX + buttonWidth / 2f,
                    h * 0.65f + buttonHeight
                )

                val rebornBorderPaint = Paint().apply {
                    color = Color.CYAN
                    style = Paint.Style.STROKE
                    strokeWidth = 6f
                    isAntiAlias = true
                    setShadowLayer(20f, 0f, 0f, Color.CYAN)
                }

                val dieBorderPaint = Paint().apply {
                    color = Color.RED
                    style = Paint.Style.STROKE
                    strokeWidth = 6f
                    isAntiAlias = true
                    setShadowLayer(20f, 0f, 0f, Color.RED)
                }

                val rebornTextPaint = Paint().apply {
                    color = Color.CYAN
                    textSize = 60f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    isAntiAlias = true
                }

                val dieTextPaint = Paint().apply {
                    color = Color.RED
                    textSize = 60f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    isAntiAlias = true
                }

                // Draw buttons
                canvas.drawRoundRect(rebornButtonRect, 20f, 20f, cardPaint)
                canvas.drawRoundRect(rebornButtonRect, 20f, 20f, rebornBorderPaint)
                canvas.drawText(
                    "⚙ BE REBORN",
                    rebornButtonRect.centerX(),
                    rebornButtonRect.centerY() + 20f,
                    rebornTextPaint
                )

                canvas.drawRoundRect(dieButtonRect, 20f, 20f, cardPaint)
                canvas.drawRoundRect(dieButtonRect, 20f, 20f, dieBorderPaint)
                canvas.drawText(
                    "☠ DIE",
                    dieButtonRect.centerX(),
                    dieButtonRect.centerY() + 20f,
                    dieTextPaint
                )
            }
        }

        // Damage flash overlay
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
