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

    // Entity caps for performance and balance
    companion object {
        private const val MAX_ENEMIES = 35
        private const val MAX_BULLETS = 200
        private const val MAX_POWERUPS = 12
        private const val MAX_BLOOD_PARTICLES = 150
    }

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

    // Pause menu system
    private var pauseMenuOpen = false
    private var pauseMenuTabRect = RectF()
    private var pauseMenuRect = RectF()
    private var pauseMenuSlideProgress = 0f  // 0 = closed, 1 = open
    private var unpauseCountdown = 0  // 3, 2, 1, 0 (0 = not counting)
    private var unpauseCountdownAlpha = 0f

    // Settings icon (moved to bottom-left)
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
        maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL) // Sharper glow
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
        maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL) // Bigger glow for player bullets
    }
    private val enemyBulletPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val enemyBulletGlowPaint = Paint().apply {
        color = Color.RED
        isAntiAlias = true
        maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
    }
    private val powerUpPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val powerUpGlowPaint = Paint().apply {
        color = Color.YELLOW
        isAntiAlias = true
        maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
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

    // Additional paints for UI elements (moved out of draw loop for performance)
    private val wallPaint = Paint().apply {
        color = Color.rgb(255, 105, 180)  // Hot pink
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val wallBorderPaint = Paint().apply {
        color = Color.rgb(255, 20, 147)  // Deep pink
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    private val greenOrbPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val greenOrbGlowPaint = Paint().apply {
        color = Color.GREEN
        isAntiAlias = true
        maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
    }
    private val powerUpIconPaint = Paint().apply {
        color = Color.BLACK
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isAntiAlias = true
    }
    private val currencyPaint = Paint().apply {
        color = Color.GREEN
        textSize = 32f
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isAntiAlias = true
    }.apply {
        setShadowLayer(8f, 0f, 0f, Color.GREEN)
    }
    private val buttonBgPaint = Paint().apply {
        color = Color.argb(120, 0, 100, 0)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val buttonBorderPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    private val buttonTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 18f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val pauseBgPaint = Paint().apply {
        color = Color.argb(80, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val pauseIconPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val pauseIconBorderPaint = Paint().apply {
        color = Color.argb(100, 0, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    // Player sprite animation
    private val playerIdleSprite: Bitmap
    private val playerRunSpriteLeft: Bitmap
    private val playerRunSpriteRight: Bitmap
    private val playerHitSprite: Bitmap
    private val playerDeathSprite: Bitmap

    // Enemy sprite
    private val enemyDroneSprite: Bitmap
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
        color = Color.argb(25, 0, 255, 255) // 1/10th opacity (was 255 alpha)
        isAntiAlias = true
        maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
    }

    // Player
    private var playerX = 0f
    private var playerY = 0f
    private var playerRadius = 60f // Tightened to match sprite borders (~70% of height)
    private var playerSpeed = 250f  // Reduced from 300
    private var playerHp = 100
    private var maxHp = 100
    private var playerFacingLeft = false // Track sprite facing direction

    // Stats (rebalanced for difficulty)
    private var bulletDamage = 8f
    private var fireRate = 1.5f
    private var fireCooldown = 0f

    // Power-up states (stackable ones use counters)
    private var hasSpreadShot = false
    private var hasRapidFire = false
    private var hasPiercing = false
    private var hasHoming = false
    private var speedBoostStacks = 0
    private var hasGiantBullets = false
    private var hasBouncyShots = false
    private var hasExplosiveRounds = false
    private var vampireStacks = 0
    private var shieldCount = 0
    private var hasMagnet = false
    private var hasBulletTime = false
    private var bulletTimeActive = false
    private var bulletTimeTimer = 0f
    private var hasOrbital = false
    private var orbitalAngle = 0f
    private var hasLaserSight = false
    private var multishotStacks = 0
    private var hasShockwave = false

    // Kill tracking
    private var killCount = 0
    private val prefs = context.getSharedPreferences("game_stats", Context.MODE_PRIVATE)
    private val settingsPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

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

    enum class EnemyType { CIRCLE, TRIANGLE, SQUARE, PENTAGON, HEXAGON }

    data class Enemy(
        var x: Float,
        var y: Float,
        var radius: Float,
        var speed: Float,
        var hp: Float,
        val maxHp: Float,
        val type: EnemyType,
        var shootCooldown: Float = 0f,
        var animFrame: Int = 0,
        var animTime: Float = 0f
    ) {
        fun getCornerCount(): Int = when(type) {
            EnemyType.CIRCLE -> 0
            EnemyType.TRIANGLE -> 3
            EnemyType.SQUARE -> 4
            EnemyType.PENTAGON -> 5
            EnemyType.HEXAGON -> 6
        }

        fun getGlowRadius(): Float {
            val hpRatio = (hp / maxHp).coerceIn(0f, 1f)
            return 10f * hpRatio  // Glow shrinks from 10f to 0f as enemy takes damage
        }

        fun getDropChance(): Float = when(type) {
            EnemyType.CIRCLE -> 0.10f      // 10% - common enemy
            EnemyType.TRIANGLE -> 0.15f    // 15%
            EnemyType.SQUARE -> 0.20f      // 20%
            EnemyType.PENTAGON -> 0.25f    // 25%
            EnemyType.HEXAGON -> 0.35f     // 35% - rare and tough
        }
    }

    data class Bullet(var x: Float, var y: Float, var vx: Float, var vy: Float, var isPlayerBullet: Boolean = true)
    data class BloodParticle(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float)
    data class Wall(val x: Float, val y: Float, val width: Float, val height: Float)

    enum class PowerUpType {
        // Original powerups
        SPREAD_SHOT, RAPID_FIRE, PIERCING, HOMING,
        // New gameplay modifiers
        SPEED_BOOST,      // Increases player movement speed (stackable)
        GIANT_BULLETS,    // Makes bullets larger with more impact
        BOUNCY_SHOTS,     // Bullets bounce off screen edges
        EXPLOSIVE_ROUNDS, // Bullets create small explosion on hit
        VAMPIRE,          // Heal 1 HP per enemy kill (stackable)
        SHIELD,           // Gain 1 shield that absorbs one hit
        MAGNET,           // Attracts power-ups from farther away
        BULLET_TIME,      // Slow down time briefly when hit
        ORBITAL,          // Spinning bullet orbits around player
        LASER_SIGHT,      // Auto-aim assistance
        MULTISHOT,        // Fire an additional bullet (stackable)
        SHOCKWAVE         // Create damaging wave on kill
    }
    data class PowerUp(var x: Float, var y: Float, val type: PowerUpType, var vy: Float = 150f)
    data class GreenOrb(var x: Float, var y: Float, var vx: Float = 0f, var vy: Float = 0f)

    private val enemies = mutableListOf<Enemy>()
    private val bullets = mutableListOf<Bullet>()
    private val blood = mutableListOf<BloodParticle>()
    private val powerUps = mutableListOf<PowerUp>()
    private val guaranteedDropEnemies = mutableSetOf<Enemy>()
    private val walls = mutableListOf<Wall>()
    private val greenOrbs = mutableListOf<GreenOrb>()

    // Clicker currency and upgrades
    private var orbCurrency = 0
    private var orbitalCount = 0
    private var clickerDamageLevel = 0
    private var clickerFireRateLevel = 0
    private var clickerSpeedLevel = 0
    private var clickerHpLevel = 0

    // Waves + gacha
    private var wave = 1
    private var inGacha = false
    private var gachaButtonsDisabled = false
    private var gachaButtonEnableTimer = 0f

    // Death screen
    private var inDeathScreen = false
    private var deathScreenFadeAlpha = 0f
    private var rebornButtonRect = RectF()
    private var dieButtonRect = RectF()

    // Clicker button rects
    private var damageButtonRect = RectF()
    private var fireButtonRect = RectF()
    private var speedButtonRect = RectF()
    private var hpButtonRect = RectF()

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
        playerRunSpriteLeft = BitmapFactory.decodeResource(resources, R.drawable.run_left)
        playerRunSpriteRight = BitmapFactory.decodeResource(resources, R.drawable.run_right)
        playerHitSprite = BitmapFactory.decodeResource(resources, R.drawable.player_attack) // Using attack as hit
        playerDeathSprite = BitmapFactory.decodeResource(resources, R.drawable.player_death)

        // Load enemy sprites
        enemyDroneSprite = BitmapFactory.decodeResource(resources, R.drawable.enemy_drone)
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

            // Settings icon in bottom-left corner (comfortable one-handed reach)
            val iconSize = 70f
            val margin = 30f
            settingsIconRect = RectF(
                margin,
                h - iconSize - margin,
                margin + iconSize,
                h - margin
            )

            // Pause menu tab on right edge (middle of screen)
            val tabWidth = 50f
            val tabHeight = 120f
            pauseMenuTabRect = RectF(
                w - tabWidth,
                h / 2f - tabHeight / 2f,
                w.toFloat(),
                h / 2f + tabHeight / 2f
            )

            // Pause menu flyout (slides in from right)
            val menuWidth = w * 0.85f
            pauseMenuRect = RectF(
                w.toFloat(),  // Starts off-screen right
                0f,
                w + menuWidth,
                h.toFloat()
            )

            spawnWave()

            // Start audio
            AudioManager.startMusic(context)
            AudioManager.startRain(context)

            // Workaround: Restart music after 3 seconds to ensure it plays
            postDelayed({
                AudioManager.stopMusic()
                AudioManager.startMusic(context)
            }, 3000)

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

        val rnd = Random(System.currentTimeMillis())
        val isBreatherWave = wave % 5 == 0 && wave > 0

        // Logarithmic scaling with soft cap at MAX_ENEMIES
        val baseCount = 8 + (wave * 3f) / (1f + wave * 0.05f)
        val count = if (isBreatherWave) {
            // Breather wave: 60% normal count but guaranteed loot
            (baseCount * 0.6f).toInt()
        } else {
            min(baseCount.toInt(), MAX_ENEMIES)
        }

        for (i in 0 until count) {
            val edge = rnd.nextInt(4)
            val ex: Float
            val ey: Float
            // Spawn in world coordinates relative to player position
            when (edge) {
                0 -> { ex = playerX - width/2f + rnd.nextFloat() * width; ey = playerY - height/2f - 60f }
                1 -> { ex = playerX - width/2f + rnd.nextFloat() * width; ey = playerY + height/2f + 60f }
                2 -> { ex = playerX - width/2f - 60f; ey = playerY - height/2f + rnd.nextFloat() * height }
                else -> { ex = playerX + width/2f + 60f; ey = playerY - height/2f + rnd.nextFloat() * height }
            }

            // Logarithmic HP scaling (gentler early game)
            val hp = 20f + sqrt(wave.toFloat()) * 15f  // Reduced from 30 + 20

            // Speed caps at 350f, starts slower
            val baseSpeed = min(80f + wave * 8f, 350f)  // Starts at 80 instead of 100

            // Elite enemies after wave 10: 20% chance for 2x HP, guaranteed drop
            val isElite = wave >= 10 && rnd.nextFloat() < 0.2f
            val finalHp = if (isElite) hp * 2f else hp

            // Progressive polygon introduction based on wave
            val enemyType = when {
                wave <= 2 -> EnemyType.CIRCLE
                wave <= 4 -> if (rnd.nextFloat() < 0.7f) EnemyType.CIRCLE else EnemyType.TRIANGLE
                wave <= 6 -> when (rnd.nextInt(3)) {
                    0 -> EnemyType.CIRCLE
                    1 -> EnemyType.TRIANGLE
                    else -> EnemyType.SQUARE
                }
                wave <= 9 -> when (rnd.nextInt(4)) {
                    0 -> EnemyType.CIRCLE
                    1 -> EnemyType.TRIANGLE
                    2 -> EnemyType.SQUARE
                    else -> EnemyType.PENTAGON
                }
                else -> when (rnd.nextInt(5)) {
                    0 -> EnemyType.CIRCLE
                    1 -> EnemyType.TRIANGLE
                    2 -> EnemyType.SQUARE
                    3 -> EnemyType.PENTAGON
                    else -> EnemyType.HEXAGON
                }
            }

            val enemy = Enemy(ex, ey, 24f, baseSpeed, finalHp, finalHp, enemyType)

            // Tag elite enemies for guaranteed drops (store in a set)
            if (isElite || isBreatherWave) {
                guaranteedDropEnemies.add(enemy)
            }

            enemies.add(enemy)
        }

        // Spawn 3-6 random pink wall obstacles per wave
        walls.clear()
        val wallCount = 3 + rnd.nextInt(4)  // 3-6 walls
        for (i in 0 until wallCount) {
            // Random orientation: 50% horizontal, 50% vertical
            val isHorizontal = rnd.nextBoolean()
            val wallWidth = if (isHorizontal) {
                50f + rnd.nextFloat() * 100f  // Horizontal: 50-150px wide
            } else {
                20f  // Vertical: 20px wide
            }
            val wallHeight = if (isHorizontal) {
                20f  // Horizontal: 20px tall
            } else {
                50f + rnd.nextFloat() * 100f  // Vertical: 50-150px tall
            }

            // Try to spawn wall away from player (max 10 attempts)
            var wx = 0f
            var wy = 0f
            var attempts = 0
            val minDistFromPlayer = 150f  // Minimum distance from player

            do {
                wx = playerX - width/3f + rnd.nextFloat() * (width * 2f/3f)
                wy = playerY - height/3f + rnd.nextFloat() * (height * 2f/3f)

                // Check if wall center is far enough from player
                val wallCenterX = wx + wallWidth / 2f
                val wallCenterY = wy + wallHeight / 2f
                val distToPlayer = sqrt((wallCenterX - playerX) * (wallCenterX - playerX) +
                                       (wallCenterY - playerY) * (wallCenterY - playerY))

                if (distToPlayer >= minDistFromPlayer) break
                attempts++
            } while (attempts < 10)

            // Only add wall if we found a valid position
            if (attempts < 10) {
                walls.add(Wall(wx, wy, wallWidth, wallHeight))
            }
        }

        if (playerHp <= 0) {
            playerHp = maxHp
        }
        inGacha = false
    }

    private fun update(dt: Float) {
        // Handle pause menu animation
        val targetSlideProgress = if (pauseMenuOpen) 1f else 0f
        pauseMenuSlideProgress += (targetSlideProgress - pauseMenuSlideProgress) * 10f * dt
        pauseMenuSlideProgress = pauseMenuSlideProgress.coerceIn(0f, 1f)

        // Handle unpause countdown
        if (unpauseCountdown > 0) {
            unpauseCountdownAlpha -= dt * 1.5f
            if (unpauseCountdownAlpha <= 0f) {
                unpauseCountdown--
                if (unpauseCountdown > 0) {
                    unpauseCountdownAlpha = 1f
                }
            }
        }

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

        // Pause game when menu is open or countdown is running
        if (pauseMenuOpen || unpauseCountdown > 0) {
            return
        }

        if (!inGacha) {
            updatePlayer(dt)
            updateEnemies(dt)
            updateBullets(dt)
            updatePowerUps(dt)
            handleAutoFire(dt)
            handleEnemyShooting(dt)
        }

        updateBlood(dt)
        updateScreenEffects(dt)
        updateGreenOrbs(dt)

        // Update gacha button enable timer
        if (gachaButtonsDisabled && gachaButtonEnableTimer > 0f) {
            gachaButtonEnableTimer -= dt
            if (gachaButtonEnableTimer <= 0f) {
                gachaButtonsDisabled = false
            }
        }

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
                val ny = joyDy / len  // Fixed: was joyDx
                // Apply speed boost stacks (10% per stack)
                val speedMultiplier = 1f + (speedBoostStacks * 0.10f)
                val newX = playerX + nx * playerSpeed * speedMultiplier * dt
                val newY = playerY + ny * playerSpeed * speedMultiplier * dt

                // Check wall collision
                var blockedByWall = false
                for (wall in walls) {
                    if (newX - playerRadius < wall.x + wall.width &&
                        newX + playerRadius > wall.x &&
                        newY - playerRadius < wall.y + wall.height &&
                        newY + playerRadius > wall.y) {
                        blockedByWall = true
                        break
                    }
                }

                if (!blockedByWall) {
                    playerX = newX
                    playerY = newY
                }
                isMoving = true

                // Track facing direction for sprite flipping
                if (joyDx < -0.1f) {
                    playerFacingLeft = true
                } else if (joyDx > 0.1f) {
                    playerFacingLeft = false
                }
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
                val newX = e.x + nx * e.speed * dt
                val newY = e.y + ny * e.speed * dt

                // Simple wall pathfinding: if blocked, try sliding along wall
                var blockedByWall = false
                for (wall in walls) {
                    if (newX - e.radius < wall.x + wall.width &&
                        newX + e.radius > wall.x &&
                        newY - e.radius < wall.y + wall.height &&
                        newY + e.radius > wall.y) {
                        blockedByWall = true
                        break
                    }
                }

                if (!blockedByWall) {
                    e.x = newX
                    e.y = newY
                } else {
                    // Try moving just in X or Y direction to slide around wall
                    val tryX = e.x + nx * e.speed * dt
                    var canMoveX = true
                    for (wall in walls) {
                        if (tryX - e.radius < wall.x + wall.width &&
                            tryX + e.radius > wall.x &&
                            e.y - e.radius < wall.y + wall.height &&
                            e.y + e.radius > wall.y) {
                            canMoveX = false
                            break
                        }
                    }
                    if (canMoveX) {
                        e.x = tryX
                    } else {
                        // Try moving just in Y
                        val tryY = e.y + ny * e.speed * dt
                        var canMoveY = true
                        for (wall in walls) {
                            if (e.x - e.radius < wall.x + wall.width &&
                                e.x + e.radius > wall.x &&
                                tryY - e.radius < wall.y + wall.height &&
                                tryY + e.radius > wall.y) {
                                canMoveY = false
                                break
                            }
                        }
                        if (canMoveY) {
                            e.y = tryY
                        }
                    }
                }

                // Update animation
                e.animTime += dt
                if (e.animTime >= 0.2f) {  // 5 FPS animation
                    e.animTime = 0f
                    e.animFrame = (e.animFrame + 1) % 3
                }
            }

            // Update shoot cooldown
            if (e.shootCooldown > 0f) {
                e.shootCooldown -= dt
            }

            if (dist < e.radius + playerRadius && damageCooldown <= 0f) {
                // Shield absorbs damage
                if (shieldCount > 0) {
                    shieldCount--
                    damageCooldown = 0.5f
                } else {
                    // Increased damage from 15 to 25 per second, but with cooldown
                    playerHp -= 25
                    if (playerHp < 0) playerHp = 0
                    tookDamage = true
                    damageCooldown = 0.5f // Half second between damage ticks

                    // Bullet time activates when hit
                    if (hasBulletTime) {
                        bulletTimeActive = true
                        bulletTimeTimer = 2f // 2 seconds of slow-mo
                    }
                }

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

            // Check wall collision - bullets are blocked
            var hitWall = false
            for (wall in walls) {
                if (b.x >= wall.x && b.x <= wall.x + wall.width &&
                    b.y >= wall.y && b.y <= wall.y + wall.height) {
                    hitWall = true
                    break
                }
            }
            if (hitWall) {
                bulletIt.remove()
                continue
            }

            // Remove bullets that are far from player (world coordinates, not screen coordinates)
            val dx = b.x - playerX
            val dy = b.y - playerY
            val distFromPlayer = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (distFromPlayer > 2000f) {
                bulletIt.remove()
                continue
            }

            if (b.isPlayerBullet) {
                // Player bullet - check enemy hits
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
                    if (!hasPiercing) {
                        bulletIt.remove() // Remove bullet unless piercing
                    }
                    if (hitEnemy.hp <= 0f) {
                        // Check if powerup cap allows spawning
                        val canSpawnPowerup = powerUps.size < MAX_POWERUPS

                        // Guaranteed drop for elite/breather enemies, or normal chance
                        val shouldDrop = if (guaranteedDropEnemies.contains(hitEnemy)) {
                            guaranteedDropEnemies.remove(hitEnemy)
                            true
                        } else {
                            Random.nextFloat() < hitEnemy.getDropChance()
                        }

                        // Only spawn powerups if enabled in settings
                        val powerupsEnabled = settingsPrefs.getBoolean("powerups_enabled", true)
                        if (shouldDrop && canSpawnPowerup && powerupsEnabled) {
                            val powerUpType = PowerUpType.values()[Random.nextInt(PowerUpType.values().size)]
                            powerUps.add(PowerUp(hitEnemy.x, hitEnemy.y, powerUpType))
                        }

                        // 60% chance to drop green orb currency
                        if (Random.nextFloat() < 0.6f) {
                            greenOrbs.add(GreenOrb(hitEnemy.x, hitEnemy.y))
                        }

                        // Vampire effect - heal on kill
                        if (vampireStacks > 0) {
                            playerHp = min(playerHp + vampireStacks, maxHp)
                        }

                        // Shockwave effect - damage nearby enemies
                        if (hasShockwave) {
                            for (nearEnemy in enemies) {
                                val shockDist = hypot((nearEnemy.x - hitEnemy.x).toDouble(), (nearEnemy.y - hitEnemy.y).toDouble()).toFloat()
                                if (shockDist < 150f && nearEnemy != hitEnemy) {
                                    nearEnemy.hp -= bulletDamage * 0.5f
                                }
                            }
                        }

                        enemies.remove(hitEnemy)
                        killCount++
                    }
                }
            } else {
                // Enemy bullet - check player hit
                val dx = b.x - playerX
                val dy = b.y - playerY
                val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                if (dist < playerRadius && damageCooldown <= 0f) {
                    playerHp -= 15 // Enemy bullets do less damage
                    if (playerHp < 0) playerHp = 0
                    triggerDamageFeedback()
                    damageCooldown = 0.3f
                    bulletIt.remove()

                    // Trigger hit animation
                    isPlayingHitAnimation = true
                    hitAnimationTime = 0f
                    currentFrame = 0
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
                // Check bullet cap before firing
                if (bullets.size >= MAX_BULLETS) {
                    fireCooldown = 0.1f // Try again very soon
                    return
                }

                val nx = dx / dist
                val ny = dy / dist
                val bulletSpeed = 700f

                // Calculate number of shots (base + multishot stacks)
                val totalShots = 1 + multishotStacks

                // Apply power-ups
                if (hasSpreadShot) {
                    // Fire 3 bullets in a spread per shot
                    val spreadAngle = 0.3f
                    for (shot in 0 until totalShots) {
                        val angleOffset = if (totalShots > 1) (shot - totalShots / 2f) * 0.2f else 0f
                        for (i in -1..1) {
                            if (bullets.size >= MAX_BULLETS) break
                            val angle = atan2(ny.toDouble(), nx.toDouble()).toFloat() + i * spreadAngle + angleOffset
                            val vx = cos(angle) * bulletSpeed
                            val vy = sin(angle) * bulletSpeed
                            bullets.add(Bullet(playerX, playerY, vx, vy, true))
                        }
                    }
                } else {
                    // Fire multiple shots in a tight spread
                    for (shot in 0 until totalShots) {
                        if (bullets.size >= MAX_BULLETS) break
                        val angleOffset = if (totalShots > 1) (shot - totalShots / 2f) * 0.15f else 0f
                        val angle = atan2(ny.toDouble(), nx.toDouble()).toFloat() + angleOffset
                        val vx = cos(angle) * bulletSpeed
                        val vy = sin(angle) * bulletSpeed
                        bullets.add(Bullet(playerX, playerY, vx, vy, true))
                    }
                }

                val actualFireRate = if (hasRapidFire) fireRate * 1.5f else fireRate
                fireCooldown = 1f / actualFireRate
            }
        }
    }

    private fun handleEnemyShooting(dt: Float) {
        // Dynamic cooldown based on enemy count to reduce bullet spam
        val crowdPenalty = (enemies.size / 25f).coerceIn(0f, 3f)

        for (e in enemies) {
            val cornerCount = e.getCornerCount()

            if (e.shootCooldown <= 0f) {
                // Check bullet cap before allowing shooting
                if (bullets.size >= MAX_BULLETS) {
                    e.shootCooldown = 1f // Try again in 1 second
                    continue
                }

                if (cornerCount == 0) {
                    // Circles fire one slow shot toward player after delay
                    val dx = playerX - e.x
                    val dy = playerY - e.y
                    val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                    if (dist > 0f) {
                        val bulletSpeed = 150f // Slower than polygon bullets
                        val vx = (dx / dist) * bulletSpeed
                        val vy = (dy / dist) * bulletSpeed
                        bullets.add(Bullet(e.x, e.y, vx, vy, false))
                    }
                    e.shootCooldown = 4f + crowdPenalty // Longer when crowded
                } else {
                    // Polygon enemies shoot bullets equal to corner count
                    val angleStep = (2f * PI.toFloat()) / cornerCount
                    for (i in 0 until cornerCount) {
                        if (bullets.size >= MAX_BULLETS) break
                        val angle = angleStep * i
                        val bulletSpeed = 200f // Slow dodgeable bullets
                        val vx = cos(angle) * bulletSpeed
                        val vy = sin(angle) * bulletSpeed
                        bullets.add(Bullet(e.x, e.y, vx, vy, false))
                    }
                    e.shootCooldown = 3f + crowdPenalty // Longer when crowded
                }
            }
        }
    }

    private fun updatePowerUps(dt: Float) {
        val it = powerUps.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.y += p.vy * dt // Fall down

            // Check if player collected it
            val dx = p.x - playerX
            val dy = p.y - playerY
            val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            // Magnet effect pulls power-ups closer
            val magnetRadius = if (hasMagnet) 200f else 35f
            if (dist < playerRadius + magnetRadius) {
                if (hasMagnet && dist > playerRadius + 35f) {
                    // Pull toward player
                    val pullSpeed = 400f
                    p.x += -(dx / dist) * pullSpeed * dt
                    p.y += -(dy / dist) * pullSpeed * dt
                } else {
                    // Collected - apply power-up
                    when (p.type) {
                        PowerUpType.SPREAD_SHOT -> hasSpreadShot = true
                        PowerUpType.RAPID_FIRE -> hasRapidFire = true
                        PowerUpType.PIERCING -> hasPiercing = true
                        PowerUpType.HOMING -> hasHoming = true
                        PowerUpType.SPEED_BOOST -> speedBoostStacks++
                        PowerUpType.GIANT_BULLETS -> hasGiantBullets = true
                        PowerUpType.BOUNCY_SHOTS -> hasBouncyShots = true
                        PowerUpType.EXPLOSIVE_ROUNDS -> hasExplosiveRounds = true
                        PowerUpType.VAMPIRE -> vampireStacks++
                        PowerUpType.SHIELD -> shieldCount++
                        PowerUpType.MAGNET -> {
                            hasMagnet = true
                            // Collect all orbs on the map instantly
                            val orbsCollected = greenOrbs.size
                            orbCurrency += orbsCollected
                            greenOrbs.clear()
                        }
                        PowerUpType.BULLET_TIME -> hasBulletTime = true
                        PowerUpType.ORBITAL -> hasOrbital = true
                        PowerUpType.LASER_SIGHT -> hasLaserSight = true
                        PowerUpType.MULTISHOT -> multishotStacks++
                        PowerUpType.SHOCKWAVE -> hasShockwave = true
                    }
                    it.remove()
                }
            } else if (p.y > playerY + height / 2f + 100f) {
                // Remove if off screen
                it.remove()
            }
        }
    }

    private fun updateGreenOrbs(dt: Float) {
        val it = greenOrbs.iterator()
        while (it.hasNext()) {
            val orb = it.next()

            // Check distance to player
            val dx = playerX - orb.x
            val dy = playerY - orb.y
            val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()

            // Seek player if within generous radius (300f)
            if (dist < 300f) {
                val seekSpeed = 250f
                orb.vx = (dx / dist) * seekSpeed
                orb.vy = (dy / dist) * seekSpeed
                orb.x += orb.vx * dt
                orb.y += orb.vy * dt

                // Collect if touching player
                if (dist < playerRadius + 15f) {
                    orbCurrency++
                    it.remove()
                }
            } else if (orb.y > playerY + height / 2f + 100f) {
                // Remove if too far off screen
                it.remove()
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
            // Save clicker data
            putInt("orb_currency", orbCurrency)
            putInt("clicker_damage_level", clickerDamageLevel)
            putInt("clicker_fire_rate_level", clickerFireRateLevel)
            putInt("clicker_speed_level", clickerSpeedLevel)
            putInt("clicker_hp_level", clickerHpLevel)
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

            // Load clicker data
            orbCurrency = prefs.getInt("orb_currency", 0)
            clickerDamageLevel = prefs.getInt("clicker_damage_level", 0)
            clickerFireRateLevel = prefs.getInt("clicker_fire_rate_level", 0)
            clickerSpeedLevel = prefs.getInt("clicker_speed_level", 0)
            clickerHpLevel = prefs.getInt("clicker_hp_level", 0)

            // Spawn enemies for current wave using spawnWave()
            spawnWave()
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
        powerUps.clear()

        // Reset power-up states
        hasSpreadShot = false
        hasRapidFire = false
        hasPiercing = false
        hasHoming = false

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
            enemies.add(Enemy(ex, ey, 24f, baseSpeed, hp, hp, EnemyType.CIRCLE))
        }
    }

    private fun spawnBlood(x: Float, y: Float) {
        // Respect blood particle cap for performance
        if (blood.size >= MAX_BLOOD_PARTICLES) {
            // Remove oldest particles to make room
            if (blood.isNotEmpty()) {
                blood.removeAt(0)
            }
        }

        val rnd = Random(System.nanoTime())
        val particleCount = min(12, MAX_BLOOD_PARTICLES - blood.size)
        for (i in 0 until particleCount) {
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

        // Disable buttons for 1 second to prevent accidental clicks
        gachaButtonsDisabled = true
        gachaButtonEnableTimer = 1f

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
                        UpgradeOption(t, "Damage +12%", "Bullets hit harder.")
                    )
                UpgradeType.FIRE_RATE ->
                    upgradeOptions.add(
                        UpgradeOption(t, "Fire Rate +12%", "Shoot more often.")
                    )
                UpgradeType.SPEED ->
                    upgradeOptions.add(
                        UpgradeOption(t, "Speed +10%", "Move faster.")
                    )
                UpgradeType.HP ->
                    upgradeOptions.add(
                        UpgradeOption(t, "HP Boost", "Max HP +8 and heal.")
                    )
            }
        }
    }

    private fun applyUpgrade(option: UpgradeOption) {
        when (option.type) {
            UpgradeType.DAMAGE -> bulletDamage *= 1.12f  // Further nerfed
            UpgradeType.FIRE_RATE -> fireRate *= 1.12f  // Further nerfed
            UpgradeType.SPEED -> playerSpeed *= 1.10f  // Further nerfed
            UpgradeType.HP -> {
                maxHp += 8  // Heavily nerfed from 20
                playerHp = maxHp
            }
        }
        wave += 1
        spawnWave()
        inGacha = false

        // Start countdown like settings menu
        countdownValue = 3
        countdownAlpha = 1f
    }

    private fun handleGachaTouch(x: Float, y: Float) {
        if (!inGacha || gachaButtonsDisabled) return
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

        // Handle pause menu touches
        if (action == MotionEvent.ACTION_UP) {
            val x = event.getX(index)
            val y = event.getY(index)

            // Check pause menu tab (always visible)
            if (!inGacha && !isDying && !inDeathScreen && pauseMenuTabRect.contains(x, y)) {
                pauseMenuOpen = !pauseMenuOpen
                if (!pauseMenuOpen) {
                    // Starting unpause countdown
                    unpauseCountdown = 3
                    unpauseCountdownAlpha = 1f
                }
                return true
            }

            // Check clicker upgrade buttons inside pause menu
            if (pauseMenuOpen && !inGacha && !isDying && !inDeathScreen) {
                val cost = 10
                when {
                    damageButtonRect.contains(x, y) && orbCurrency >= cost -> {
                        orbCurrency -= cost
                        clickerDamageLevel++
                        bulletDamage += bulletDamage * 0.01f // +1% permanent
                        return true
                    }
                    fireButtonRect.contains(x, y) && orbCurrency >= cost -> {
                        orbCurrency -= cost
                        clickerFireRateLevel++
                        fireRate += fireRate * 0.01f // +1% permanent
                        return true
                    }
                    speedButtonRect.contains(x, y) && orbCurrency >= cost -> {
                        orbCurrency -= cost
                        clickerSpeedLevel++
                        playerSpeed += playerSpeed * 0.01f // +1% permanent
                        return true
                    }
                    hpButtonRect.contains(x, y) && orbCurrency >= cost -> {
                        orbCurrency -= cost
                        clickerHpLevel++
                        maxHp += (maxHp * 0.01f).toInt() // +1% permanent
                        playerHp += (maxHp * 0.01f).toInt() // Also heal by the increase
                        return true
                    }
                }
            }

            // Check settings icon tap (opens Settings activity)
            if (!inGacha && settingsIconRect.contains(x, y)) {
                pauseMenuOpen = false  // Close pause menu
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

                // Don't activate joystick if touching UI buttons or pause menu
                val touchingUI = !inGacha && !isDying && !inDeathScreen && (
                    pauseMenuTabRect.contains(x, y) ||
                    settingsIconRect.contains(x, y) ||
                    (pauseMenuOpen && (
                        damageButtonRect.contains(x, y) ||
                        fireButtonRect.contains(x, y) ||
                        speedButtonRect.contains(x, y) ||
                        hpButtonRect.contains(x, y)
                    ))
                )

                // Floating joystick: appears anywhere on screen (except UI buttons)
                if (joyPointerId == -1 && !touchingUI && !inGacha && !inDeathScreen) {
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

        // Draw bullets with different visuals for player vs enemy
        for (b in bullets) {
            if (b.isPlayerBullet) {
                // Player bullets - cyan diamonds with glow
                canvas.drawCircle(b.x, b.y, 15f, bulletGlowPaint)
                val path = Path().apply {
                    moveTo(b.x, b.y - 8f)
                    lineTo(b.x + 6f, b.y)
                    lineTo(b.x, b.y + 8f)
                    lineTo(b.x - 6f, b.y)
                    close()
                }
                canvas.drawPath(path, bulletPaint)
            } else {
                // Enemy bullets - red circles with glow
                canvas.drawCircle(b.x, b.y, 10f, enemyBulletGlowPaint)
                canvas.drawCircle(b.x, b.y, 5f, enemyBulletPaint)
            }
        }

        // Draw enemies with animated sprites
        for (e in enemies) {
            // Sprite sheet: 144×48 pixels = 9 columns × 3 rows of 16×16px sprites
            // Each character (row) has: down=cols 0,3,6  left=cols 1,4,7  up=cols 2,5,8
            val row = when (e.type) {
                EnemyType.CIRCLE -> 0
                EnemyType.TRIANGLE -> 1
                else -> 2  // SQUARE, PENTAGON, HEXAGON
            }

            // Determine direction based on movement toward player
            val dx = playerX - e.x
            val dy = playerY - e.y
            val absX = abs(dx)
            val absY = abs(dy)

            // Select base column for direction
            val (baseCol, flipHorizontal) = when {
                absY > absX && dy > 0 -> Pair(0, false)  // Down: cols 0,3,6
                absY > absX && dy < 0 -> Pair(2, false)  // Up: cols 2,5,8
                absX >= absY && dx < 0 -> Pair(1, false) // Left: cols 1,4,7
                else -> Pair(1, true)  // Right: flip left sprite
            }

            // 3 animation frames: baseCol, baseCol+3, baseCol+6
            val animIndex = e.animFrame % 3
            val frameCol = baseCol + (animIndex * 3)

            // Each sprite is 64×64px (testing larger size)
            val spriteSize = 64

            val srcRect = Rect(
                frameCol * spriteSize,
                row * spriteSize,
                (frameCol + 1) * spriteSize,
                (row + 1) * spriteSize
            )

            // Draw sprite (make it big enough to see clearly)
            val enemySize = 60f  // Fixed size - large enough to see 16×16px sprite clearly

            // Draw glow BEHIND sprite (smaller than sprite)
            val glowRadius = e.getGlowRadius()
            canvas.drawCircle(e.x, e.y, enemySize / 3f + glowRadius, enemyGlowPaint)

            // Flip horizontally if moving right
            if (flipHorizontal) {
                canvas.save()
                canvas.scale(-1f, 1f, e.x, e.y)
            }

            val dstRect = RectF(
                e.x - enemySize / 2f,
                e.y - enemySize / 2f,
                e.x + enemySize / 2f,
                e.y + enemySize / 2f
            )
            canvas.drawBitmap(enemyDroneSprite, srcRect, dstRect, spritePaint)

            if (flipHorizontal) {
                canvas.restore()
            }
        }

        // Draw power-ups (larger and clearer)
        for (p in powerUps) {
            canvas.drawCircle(p.x, p.y, 35f, powerUpGlowPaint)
            canvas.drawCircle(p.x, p.y, 22f, powerUpPaint)
            // Draw icon/letter for power-up type (using class-level paint)
            val icon = when (p.type) {
                PowerUpType.SPREAD_SHOT -> "S"
                PowerUpType.RAPID_FIRE -> "R"
                PowerUpType.PIERCING -> "P"
                PowerUpType.HOMING -> "H"
                PowerUpType.SPEED_BOOST -> "+"
                PowerUpType.GIANT_BULLETS -> "B"
                PowerUpType.BOUNCY_SHOTS -> "~"
                PowerUpType.EXPLOSIVE_ROUNDS -> "E"
                PowerUpType.VAMPIRE -> "V"
                PowerUpType.SHIELD -> "⬡"
                PowerUpType.MAGNET -> "M"
                PowerUpType.BULLET_TIME -> "T"
                PowerUpType.ORBITAL -> "O"
                PowerUpType.LASER_SIGHT -> "L"
                PowerUpType.MULTISHOT -> "X"
                PowerUpType.SHOCKWAVE -> "W"
            }
            canvas.drawText(icon, p.x, p.y + 10f, powerUpIconPaint)
        }

        // Draw pink walls (using class-level paints)
        for (wall in walls) {
            canvas.drawRect(wall.x, wall.y, wall.x + wall.width, wall.y + wall.height, wallPaint)
            canvas.drawRect(wall.x, wall.y, wall.x + wall.width, wall.y + wall.height, wallBorderPaint)
        }

        // Draw green orbs (currency - using class-level paints)
        for (orb in greenOrbs) {
            canvas.drawCircle(orb.x, orb.y, 20f, greenOrbGlowPaint)
            canvas.drawCircle(orb.x, orb.y, 10f, greenOrbPaint)
        }

        // Draw player sprite
        val isMoving = joyActive && hypot(joyDx.toDouble(), joyDy.toDouble()).toFloat() > 0.01f

        // Choose sprite based on state (death > hit > movement)
        val spriteSheet: Bitmap
        val totalFrames: Int
        when {
            isDying -> {
                spriteSheet = playerDeathSprite
                totalFrames = 5 // Death animation frames
            }
            isPlayingHitAnimation -> {
                spriteSheet = playerHitSprite
                totalFrames = 2 // Hit animation frames (player_attack.png used as hit)
            }
            isMoving -> {
                // Select correct sprite based on facing direction
                spriteSheet = if (playerFacingLeft) playerRunSpriteLeft else playerRunSpriteRight
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

        // Destination rect (where to draw on screen)
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

        // Draw pause menu tab on right edge (always visible)
        val tabPaint = Paint().apply {
            color = Color.argb(220, 20, 20, 40)
            isAntiAlias = true
        }
        val tabTextPaint = Paint().apply {
            color = Color.CYAN
            textSize = 36f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawRoundRect(pauseMenuTabRect, 15f, 15f, tabPaint)
        canvas.save()
        canvas.rotate(90f, pauseMenuTabRect.centerX(), pauseMenuTabRect.centerY())
        canvas.drawText("MENU", pauseMenuTabRect.centerX(), pauseMenuTabRect.centerY() + 12f, tabTextPaint)
        canvas.restore()

        // Draw flyout pause menu (slides in from right)
        if (pauseMenuSlideProgress > 0.01f) {
            val menuWidth = w * 0.85f
            val menuLeft = w - (menuWidth * pauseMenuSlideProgress)

            val menuBgPaint = Paint().apply {
                color = Color.argb(240, 10, 10, 25)
                isAntiAlias = true
            }

            canvas.drawRect(menuLeft, 0f, w, h, menuBgPaint)

            // Draw clicker UI inside pause menu
            val menuTitlePaint = Paint().apply {
                color = Color.CYAN
                textSize = 48f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("UPGRADES", menuLeft + menuWidth / 2f, 100f, menuTitlePaint)

            // Currency display
            val currencyText = "Orbs: $orbCurrency"
            canvas.drawText(currencyText, menuLeft + menuWidth / 2f, 180f, currencyPaint)

            // Upgrade buttons (vertically stacked in menu)
            val buttonWidth = menuWidth * 0.8f
            val buttonHeight = 80f
            val buttonGap = 20f
            val buttonStartX = menuLeft + (menuWidth - buttonWidth) / 2f
            var buttonY = 240f

            // Button 1: Damage
            damageButtonRect = RectF(buttonStartX, buttonY, buttonStartX + buttonWidth, buttonY + buttonHeight)
            canvas.drawRoundRect(damageButtonRect, 10f, 10f, buttonBgPaint)
            canvas.drawRoundRect(damageButtonRect, 10f, 10f, buttonBorderPaint)
            canvas.drawText("DMG+", damageButtonRect.centerX(), damageButtonRect.centerY() - 15f, buttonTextPaint)
            canvas.drawText("Level: $clickerDamageLevel", damageButtonRect.centerX(), damageButtonRect.centerY() + 5f, buttonTextPaint)
            canvas.drawText("Cost: 10 orbs", damageButtonRect.centerX(), damageButtonRect.centerY() + 25f, buttonTextPaint)

            buttonY += buttonHeight + buttonGap

            // Button 2: Fire Rate
            fireButtonRect = RectF(buttonStartX, buttonY, buttonStartX + buttonWidth, buttonY + buttonHeight)
            canvas.drawRoundRect(fireButtonRect, 10f, 10f, buttonBgPaint)
            canvas.drawRoundRect(fireButtonRect, 10f, 10f, buttonBorderPaint)
            canvas.drawText("FIRE+", fireButtonRect.centerX(), fireButtonRect.centerY() - 15f, buttonTextPaint)
            canvas.drawText("Level: $clickerFireRateLevel", fireButtonRect.centerX(), fireButtonRect.centerY() + 5f, buttonTextPaint)
            canvas.drawText("Cost: 10 orbs", fireButtonRect.centerX(), fireButtonRect.centerY() + 25f, buttonTextPaint)

            buttonY += buttonHeight + buttonGap

            // Button 3: Speed
            speedButtonRect = RectF(buttonStartX, buttonY, buttonStartX + buttonWidth, buttonY + buttonHeight)
            canvas.drawRoundRect(speedButtonRect, 10f, 10f, buttonBgPaint)
            canvas.drawRoundRect(speedButtonRect, 10f, 10f, buttonBorderPaint)
            canvas.drawText("SPD+", speedButtonRect.centerX(), speedButtonRect.centerY() - 15f, buttonTextPaint)
            canvas.drawText("Level: $clickerSpeedLevel", speedButtonRect.centerX(), speedButtonRect.centerY() + 5f, buttonTextPaint)
            canvas.drawText("Cost: 10 orbs", speedButtonRect.centerX(), speedButtonRect.centerY() + 25f, buttonTextPaint)

            buttonY += buttonHeight + buttonGap

            // Button 4: HP
            hpButtonRect = RectF(buttonStartX, buttonY, buttonStartX + buttonWidth, buttonY + buttonHeight)
            canvas.drawRoundRect(hpButtonRect, 10f, 10f, buttonBgPaint)
            canvas.drawRoundRect(hpButtonRect, 10f, 10f, buttonBorderPaint)
            canvas.drawText("HP+", hpButtonRect.centerX(), hpButtonRect.centerY() - 15f, buttonTextPaint)
            canvas.drawText("Level: $clickerHpLevel", hpButtonRect.centerX(), hpButtonRect.centerY() + 5f, buttonTextPaint)
            canvas.drawText("Cost: 10 orbs", hpButtonRect.centerX(), hpButtonRect.centerY() + 25f, buttonTextPaint)
        }

        // Settings icon in bottom-left (gear icon)
        canvas.drawCircle(settingsIconRect.centerX(), settingsIconRect.centerY(), 35f, pauseBgPaint)

        // Draw gear-like icon
        val gearPaint = Paint().apply {
            color = Color.CYAN
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        canvas.drawCircle(settingsIconRect.centerX(), settingsIconRect.centerY(), 18f, gearPaint)
        canvas.drawCircle(settingsIconRect.centerX(), settingsIconRect.centerY(), 8f, gearPaint)

        // Joystick in screen space
        if (joyActive) {
            canvas.drawCircle(joyBaseX, joyBaseY, joyBaseRadius, joyBasePaint)
            val knobX = joyBaseX + joyDx
            val knobY = joyBaseY + joyDy
            canvas.drawCircle(knobX, knobY, joyKnobRadius, joyKnobPaint)
        }

        // Unpause countdown (3, 2, 1)
        if (unpauseCountdown > 0) {
            val countdownPaint = Paint().apply {
                color = Color.CYAN
                textSize = 200f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
                alpha = (unpauseCountdownAlpha * 255).toInt().coerceIn(0, 255)
            }
            canvas.drawText(unpauseCountdown.toString(), w / 2f, h / 2f + 70f, countdownPaint)
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
            // Black to red gradient
            val deathOverlayPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, h,
                    Color.BLACK, Color.rgb(80, 0, 0),
                    Shader.TileMode.CLAMP
                )
                alpha = deathScreenFadeAlpha.toInt().coerceIn(0, 255)
            }
            canvas.drawRect(0f, 0f, w, h, deathOverlayPaint)

            if (inDeathScreen) {
                // Kill count in big neon red
                val killCountPaint = Paint().apply {
                    color = Color.RED
                    textSize = 100f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    isAntiAlias = true
                    setShadowLayer(40f, 0f, 0f, Color.RED)
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

                // Draw buttons with black backgrounds
                val buttonBgPaint = Paint().apply {
                    color = Color.BLACK
                    style = Paint.Style.FILL
                }

                canvas.drawRoundRect(rebornButtonRect, 20f, 20f, buttonBgPaint)
                canvas.drawRoundRect(rebornButtonRect, 20f, 20f, rebornBorderPaint)
                canvas.drawText(
                    "⚙ BE REBORN",
                    rebornButtonRect.centerX(),
                    rebornButtonRect.centerY() + 20f,
                    rebornTextPaint
                )

                canvas.drawRoundRect(dieButtonRect, 20f, 20f, buttonBgPaint)
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
