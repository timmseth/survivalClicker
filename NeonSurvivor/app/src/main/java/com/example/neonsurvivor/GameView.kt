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

    // Health check logging - AGGRESSIVE
    private var healthCheckTimer = 0f
    private val healthCheckInterval = 2f  // Log every 2 seconds
    private var updateCallCount = 0L

    // Crash error display
    private var crashError: Throwable? = null
    private var crashErrorText: String = ""

    // Vibration
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    // Camera offset for centering player
    private var cameraX = 0f
    private var cameraY = 0f

    // Flyout menu system
    private var currentMenu: String? = null  // null, "SETTINGS", or "UPGRADES"
    private var menuSlideProgress = 0f  // 0 = closed, 1 = open
    private var settingsTabRect = RectF()
    private var upgradesTabRect = RectF()
    private var unpauseCountdown = 0  // 3, 2, 1, 0 (0 = not counting)
    private var unpauseCountdownAlpha = 0f

    // Sprite scaling slider for debugging
    private var spriteScale = 64f  // Adjustable sprite size (64 is correct for chibi-layered.png)

    // Settings controls in flyout menu
    private var musicToggleRect = RectF()
    private var soundToggleRect = RectF()
    private var powerupsToggleRect = RectF()
    private var musicVolumeSliderRect = RectF()
    private var rainVolumeSliderRect = RectF()
    private var saveButtonRect = RectF()
    private var loadButtonRect = RectF()
    private var draggingMusicSlider = false
    private var draggingRainSlider = false

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

    // Barrier shield paints (reused, only color changes)
    private val barrierPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        alpha = 180
        isAntiAlias = true
    }
    private val barrierGlowPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        alpha = 100
        isAntiAlias = true
        maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
    }
    private val barrierPath = android.graphics.Path()  // Reusable path object
    private val bulletPath = android.graphics.Path()  // Reusable path for bullet diamonds

    // Boss HP bar paints
    private val bossHpBarBgPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }
    private val bossHpBarFillPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
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

    // Player sprite animation (loaded based on character selection)
    private val playerIdleSprite: Bitmap
    private val playerRunSprite: Bitmap
    private val playerHitSprite: Bitmap
    private val playerDeathSprite: Bitmap

    // Enemy sprites - 27 individual sprites loaded from split folder
    private val enemySprites = mutableMapOf<Int, Bitmap>()
    private val bossSprites = mutableMapOf<EnemyType, Bitmap>()
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

    // Multi-gun system
    private var gunCount = 1  // Number of parallel gun streams

    // Debug settings
    private var godMode = false

    // Kill tracking
    private var killCount = 0
    private val prefs = context.getSharedPreferences("game_stats", Context.MODE_PRIVATE)
    private val settingsPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val debugPrefs = context.getSharedPreferences("debug_settings", Context.MODE_PRIVATE)

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

    enum class EnemyType { CIRCLE, TRIANGLE, SQUARE, PENTAGON, HEXAGON, BOSS_FLYING_EYE, BOSS_GOBLIN, BOSS_MUSHROOM, BOSS_SKELETON }

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
        var animTime: Float = 0f,
        val isBoss: Boolean = false
    ) {
        fun getCornerCount(): Int = when(type) {
            EnemyType.CIRCLE -> 0
            EnemyType.TRIANGLE -> 3
            EnemyType.SQUARE -> 4
            EnemyType.PENTAGON -> 5
            EnemyType.HEXAGON -> 6
            EnemyType.BOSS_FLYING_EYE, EnemyType.BOSS_GOBLIN, EnemyType.BOSS_MUSHROOM, EnemyType.BOSS_SKELETON -> 0  // Bosses don't shoot bullets
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
            EnemyType.BOSS_FLYING_EYE, EnemyType.BOSS_GOBLIN, EnemyType.BOSS_MUSHROOM, EnemyType.BOSS_SKELETON -> 1.0f  // 100% - bosses always drop
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
    private var clickerBarrierLevel = 0

    // Barrier shield system (personal shield around player)
    private var barrierShieldLayers = 0  // Current active barrier layers (0 to clickerBarrierLevel)
    private var barrierRecoveryTimer = 0f  // Timer for recovering barrier layers
    private val barrierRecoveryDelay = 10f  // 10 seconds without damage to recover 1 layer
    private var barrierWaveOffset = 0f  // Animation offset for wavy effect
    private var barrierAoeDamageTimer = 0f  // Timer for AOE damage pulses (level 7+)

    // Waves + gacha
    private var wave = 1
    private var inGacha = false
    private var gachaButtonsDisabled = false
    private var gachaButtonEnableTimer = 0f

    // QoL: Wave completion feedback
    private var waveCompleteFeedbackTime = 0f
    private var completedWaveNumber = 0

    // QoL: Powerup pickup feedback
    private var powerupPickupFeedback = ""
    private var powerupFeedbackTime = 0f

    // QoL: Enemy proximity warning
    private var enemyProximityWarning = false

    // Debug logging system
    private val debugLogs = mutableListOf<String>()
    private val maxDebugLogs = 100  // Keep last 100 logs
    private fun addDebugLog(message: String) {
        val timestamp = System.currentTimeMillis()
        val logEntry = "[Wave $wave] $message"
        debugLogs.add(logEntry)
        if (debugLogs.size > maxDebugLogs) {
            debugLogs.removeAt(0)
        }
    }

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
    private var barrierButtonRect = RectF()

    enum class UpgradeType {
        // Special upgrades (wave-end rewards)
        STASIS_CORE, OVERCLOCKER, QUANTUM_MIRROR, FRAGMENT_DRIVE,
        CONVERT_TO_ORBS, CONVERT_TO_BULLETS, CONVERT_TO_HOMING,
        MULTI_GUN,  // Adds an additional gun
        // Basic stat upgrades removed from wave-end (moved to orb shop only)
    }

    data class UpgradeOption(val type: UpgradeType, val label: String, val desc: String, val weight: Int = 100)
    private val upgradeOptions = mutableListOf<UpgradeOption>()

    // Active special upgrade effects
    private var hasStasisCore = false
    private var hasQuantumMirror = false
    private var hasFragmentDrive = false
    private var overclockTimer = 0f
    private var overclockActive = false

    init {
        isFocusable = true
        isClickable = true

        // Enable software rendering for blur effects (BlurMaskFilter doesn't work with hardware acceleration)
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        // Load player sprites based on character selection
        val selectedCharacter = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getInt("selected_character", 0)  // 0=Biker, 1=Punk, 2=Cyborg

        val (idleRes, runRes) = when (selectedCharacter) {
            1 -> Pair(R.drawable.char_punk_idle, R.drawable.char_punk_run)
            2 -> Pair(R.drawable.char_cyborg_idle, R.drawable.char_cyborg_run)
            else -> Pair(R.drawable.char_biker_idle, R.drawable.char_biker_run)
        }

        playerIdleSprite = BitmapFactory.decodeResource(resources, idleRes)
        playerRunSprite = BitmapFactory.decodeResource(resources, runRes)
        playerHitSprite = BitmapFactory.decodeResource(resources, R.drawable.player_attack) // Using attack as hit
        playerDeathSprite = BitmapFactory.decodeResource(resources, R.drawable.player_death)

        // Load 27 individual enemy sprites from split folder
        for (i in 1..27) {
            val spriteNum = String.format("%03d", i)
            val resourceId = resources.getIdentifier("enemies$spriteNum", "drawable", context.packageName)
            if (resourceId != 0) {
                enemySprites[i] = BitmapFactory.decodeResource(resources, resourceId)
            }
        }

        // Load boss sprites
        bossSprites[EnemyType.BOSS_FLYING_EYE] = BitmapFactory.decodeResource(resources, R.drawable.boss_flyingeye)
        bossSprites[EnemyType.BOSS_GOBLIN] = BitmapFactory.decodeResource(resources, R.drawable.boss_goblin)
        bossSprites[EnemyType.BOSS_MUSHROOM] = BitmapFactory.decodeResource(resources, R.drawable.boss_mushroom)
        bossSprites[EnemyType.BOSS_SKELETON] = BitmapFactory.decodeResource(resources, R.drawable.boss_skeleton)
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

            // Menu tabs on right edge - square buttons (2x bigger for easier tapping)
            val tabSize = 160f  // Square tabs (was 80f)
            val tabGap = 40f

            // Settings tab (top)
            settingsTabRect = RectF(
                w - tabSize,
                h / 2f - tabSize - tabGap / 2f,
                w.toFloat(),
                h / 2f - tabGap / 2f
            )

            // Upgrades tab (bottom)
            upgradesTabRect = RectF(
                w - tabSize,
                h / 2f + tabGap / 2f,
                w.toFloat(),
                h / 2f + tabSize + tabGap / 2f
            )

            // Load debug settings
            godMode = debugPrefs.getBoolean("god_mode", false)
            gunCount = debugPrefs.getInt("gun_count", 1).coerceIn(1, 10)

            CrashLogger.log("GameView initialized. Starting wave 1. God mode: $godMode, Guns: $gunCount")
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
        addDebugLog("Spawning wave $wave")
        CrashLogger.log("spawnWave() called for wave $wave. Current enemies: ${enemies.size}, bullets: ${bullets.size}")
        enemies.clear()

        val rnd = Random(System.currentTimeMillis())
        val isBreatherWave = wave % 5 == 0 && wave > 0
        val isBossWave = wave % 5 == 0 && wave > 0  // Spawn boss every 5 waves
        if (isBossWave) {
            addDebugLog("Boss wave!")
            CrashLogger.log("Boss wave detected for wave $wave")
        }

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

        // Spawn boss every 5 waves
        if (isBossWave) {
            // Choose boss type based on wave number (cycle through bosses)
            val bossType = when ((wave / 5) % 4) {
                0 -> EnemyType.BOSS_FLYING_EYE
                1 -> EnemyType.BOSS_GOBLIN
                2 -> EnemyType.BOSS_MUSHROOM
                else -> EnemyType.BOSS_SKELETON
            }

            // Spawn boss in center-ish area
            val bossX = playerX + (rnd.nextFloat() - 0.5f) * 200f
            val bossY = playerY - height/2f - 100f  // Spawn above player

            // Boss stats: massive HP, slower speed, larger size
            val bossHp = 200f + wave * 50f  // Scales heavily with wave
            val bossSpeed = 40f  // Slow moving
            val bossRadius = 48f  // 2x larger than normal enemies

            val boss = Enemy(bossX, bossY, bossRadius, bossSpeed, bossHp, bossHp, bossType, isBoss = true)
            enemies.add(boss)
            guaranteedDropEnemies.add(boss)  // Bosses always drop loot
        }

        // Spawn 3-6 random pink wall obstacles per wave
        walls.clear()
        // Progressive wall count scaling with wave (starts at 2, adds 1 every 3 waves, max 12)
        val baseWallCount = 2 + (wave / 3).coerceAtMost(10)
        val wallCount = baseWallCount + rnd.nextInt(2)  // Add 0-1 random walls

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

            // Try to spawn wall away from player (max 20 attempts for better placement)
            var wx = 0f
            var wy = 0f
            var attempts = 0
            val minDistFromPlayer = 250f  // Increased safezone from 150f to 250f

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
            } while (attempts < 20)  // Increased from 10 to 20 attempts

            // Only add wall if we found a valid position
            if (attempts < 20) {
                walls.add(Wall(wx, wy, wallWidth, wallHeight))
            }
        }

        if (playerHp <= 0) {
            playerHp = maxHp
        }
        inGacha = false

        CrashLogger.log("spawnWave() COMPLETED for wave $wave. Final enemy count: ${enemies.size}, walls: ${walls.size}")
    }

    private fun update(dt: Float) {
        updateCallCount++

        // AGGRESSIVE logging every 2 seconds
        healthCheckTimer += dt
        if (healthCheckTimer >= healthCheckInterval) {
            healthCheckTimer = 0f
            CrashLogger.log("HEARTBEAT #$updateCallCount - Wave: $wave, HP: $playerHp/$maxHp, Kills: $killCount, Enemies: ${enemies.size}, Bullets: ${bullets.size}, PowerUps: ${powerUps.size}, inGacha: $inGacha")
        }

        // Handle menu slide animation
        val targetSlideProgress = if (currentMenu != null) 1f else 0f
        menuSlideProgress += (targetSlideProgress - menuSlideProgress) * 10f * dt
        menuSlideProgress = menuSlideProgress.coerceIn(0f, 1f)

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
        if (currentMenu != null || unpauseCountdown > 0) {
            return
        }

        if (!inGacha) {
            updatePlayer(dt)
            updateEnemies(dt)
            updateBullets(dt)
            updatePowerUps(dt)
            handleAutoFire(dt)
            handleEnemyShooting(dt)

            // Update Overclocker timer
            if (overclockActive) {
                overclockTimer -= dt
                if (overclockTimer <= 0f) {
                    overclockActive = false
                    overclockTimer = 0f
                }
            }

            // Update barrier shield recovery (max 7 layers, but AOE keeps scaling)
            val maxLayers = min(clickerBarrierLevel, 7)
            if (barrierShieldLayers < maxLayers) {
                barrierRecoveryTimer += dt
                if (barrierRecoveryTimer >= barrierRecoveryDelay) {
                    barrierShieldLayers++
                    barrierRecoveryTimer = 0f
                }
            }

            // Update barrier wave animation
            barrierWaveOffset += dt * 2f

            // Prismatic Barrier AOE damage (level 7+)
            if (clickerBarrierLevel >= 7) {
                barrierAoeDamageTimer += dt
                val aoePulseInterval = 1.5f  // Damage pulse every 1.5 seconds
                if (barrierAoeDamageTimer >= aoePulseInterval) {
                    barrierAoeDamageTimer = 0f
                    // AOE damage scales with levels past 7 (5 damage per level)
                    val aoeDamage = (clickerBarrierLevel - 6) * 5f
                    val aoeRadius = 150f  // Damage radius around player

                    // Damage all enemies within radius
                    for (e in enemies) {
                        val dx = e.x - playerX
                        val dy = e.y - playerY
                        val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                        if (dist < aoeRadius) {
                            e.hp -= aoeDamage
                            // Small visual feedback - spawn blood particles
                            spawnBlood(e.x, e.y)
                        }
                    }
                }
            }
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

        // QoL: Update feedback timers
        if (waveCompleteFeedbackTime > 0f) {
            waveCompleteFeedbackTime -= dt
        }
        if (powerupFeedbackTime > 0f) {
            powerupFeedbackTime -= dt
        }

        // QoL: Check enemy proximity for warning
        enemyProximityWarning = false
        if (!inGacha) {
            val dangerRadius = 150f
            for (e in enemies) {
                val dx = e.x - playerX
                val dy = e.y - playerY
                val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                if (dist < dangerRadius) {
                    enemyProximityWarning = true
                    break
                }
            }
        }

        if (!inGacha && enemies.isEmpty()) {
            CrashLogger.log("Wave $wave complete - opening gacha. Enemies: ${enemies.size}, Bullets: ${bullets.size}")
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
                // Run sprite has 6 frames (horizontal strip)
                currentFrame = (currentFrame + 1) % 6
            }
        } else {
            spriteFrameTime += dt
            if (spriteFrameTime >= frameDelay) {
                spriteFrameTime = 0f
                // Idle sprite has 4 frames (horizontal strip)
                currentFrame = (currentFrame + 1) % 4
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
                if (godMode) {
                    // God mode: deal massive damage to enemy to instantly kill them
                    e.hp -= 9999
                    // Enemy will be removed by normal enemy cleanup code
                } else {
                    // Normal damage behavior
                    // Priority: Shield powerup > Barrier shield > HP
                    if (shieldCount > 0) {
                        shieldCount--
                        damageCooldown = 0.5f
                    } else if (barrierShieldLayers > 0) {
                        // Barrier shield absorbs damage - drop one layer
                        barrierShieldLayers--
                        barrierRecoveryTimer = 0f  // Reset recovery timer
                        triggerDamageFeedback()
                        damageCooldown = 0.5f
                    } else {
                        // No shield - take HP damage
                        playerHp -= 25
                        if (playerHp < 0) playerHp = 0
                        CrashLogger.log("Player hit by enemy collision! HP: $playerHp, Enemy type: ${e.type}")
                        tookDamage = true
                        damageCooldown = 0.5f
                        barrierRecoveryTimer = 0f  // Reset recovery timer when taking HP damage

                        // Bullet time activates when hit
                        if (hasBulletTime) {
                            bulletTimeActive = true
                            bulletTimeTimer = 2f
                        }
                    }

                    // Trigger hit animation
                    isPlayingHitAnimation = true
                    hitAnimationTime = 0f
                    currentFrame = 0
                }
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
        // Temporary list for bullets spawned during iteration (Fragment Drive)
        val newBullets = mutableListOf<Bullet>()

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
                // Stasis Core - player bullets slow enemy bullets in radius
                if (hasStasisCore) {
                    val slowRadius = 100f
                    val slowFactor = 0.7f  // Slow to 70% speed (less aggressive)
                    val minSpeed = 80f  // Minimum bullet speed to prevent complete stalls
                    for (eb in bullets) {
                        if (!eb.isPlayerBullet) {
                            val dx = eb.x - b.x
                            val dy = eb.y - b.y
                            val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                            if (dist < slowRadius) {
                                // Apply slow factor
                                eb.vx *= slowFactor
                                eb.vy *= slowFactor

                                // Enforce minimum speed
                                val currentSpeed = hypot(eb.vx.toDouble(), eb.vy.toDouble()).toFloat()
                                if (currentSpeed < minSpeed && currentSpeed > 0f) {
                                    val scale = minSpeed / currentSpeed
                                    eb.vx *= scale
                                    eb.vy *= scale
                                }
                            }
                        }
                    }
                }

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

                        // Fragment Drive - spawn 4 micro-projectiles on kill
                        if (hasFragmentDrive) {
                            val fragmentSpeed = 400f
                            for (i in 0 until 4) {
                                val angle = (i * 90f + 45f) * (Math.PI / 180f).toFloat()  // 45, 135, 225, 315 degrees
                                val vx = cos(angle) * fragmentSpeed
                                val vy = sin(angle) * fragmentSpeed
                                // Add to temporary list to avoid ConcurrentModificationException
                                newBullets.add(Bullet(hitEnemy.x, hitEnemy.y, vx, vy, true))
                            }
                            CrashLogger.log("Fragment Drive queued 4 bullets at (${hitEnemy.x}, ${hitEnemy.y})")
                        }

                        enemies.remove(hitEnemy)
                        killCount++
                    }
                }
            } else {
                // Enemy bullet - check player hit and graze
                val dx = b.x - playerX
                val dy = b.y - playerY
                val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()

                // Quantum Mirror - reflect bullets that graze player (within 1.5x radius but not hitting)
                if (hasQuantumMirror && dist < playerRadius * 1.5f && dist >= playerRadius) {
                    // Reflect bullet away from player
                    val nx = dx / dist
                    val ny = dy / dist
                    val speed = hypot(b.vx.toDouble(), b.vy.toDouble()).toFloat()
                    b.vx = nx * speed
                    b.vy = ny * speed
                }

                if (dist < playerRadius && damageCooldown <= 0f) {
                    if (godMode) {
                        // God mode: just remove bullet, no damage or animations
                        bulletIt.remove()
                    } else {
                        // Barrier shield absorbs bullet damage first
                        if (barrierShieldLayers > 0) {
                            barrierShieldLayers--
                            barrierRecoveryTimer = 0f
                            triggerDamageFeedback()
                            damageCooldown = 0.3f
                        } else {
                            playerHp -= 15
                            if (playerHp < 0) playerHp = 0
                            CrashLogger.log("Player hit by enemy bullet! HP: $playerHp")
                            triggerDamageFeedback()
                            damageCooldown = 0.3f
                            barrierRecoveryTimer = 0f
                        }
                        bulletIt.remove()

                        // Trigger hit animation
                        isPlayingHitAnimation = true
                        hitAnimationTime = 0f
                        currentFrame = 0
                    }
                }
            }
        }

        // Add all queued bullets from Fragment Drive after iteration completes
        if (newBullets.isNotEmpty()) {
            bullets.addAll(newBullets)
            CrashLogger.log("Fragment Drive added ${newBullets.size} new bullets. Total bullets: ${bullets.size}")
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

                // Calculate gun positions (perpendicular to firing direction)
                val gunOffsets = mutableListOf<Pair<Float, Float>>()
                if (gunCount == 1) {
                    gunOffsets.add(Pair(0f, 0f))  // Single gun at center
                } else {
                    val gunSpacing = 25f  // Spacing between guns
                    val perpX = -ny  // Perpendicular to direction
                    val perpY = nx
                    for (i in 0 until gunCount) {
                        val offset = (i - (gunCount - 1) / 2f) * gunSpacing
                        gunOffsets.add(Pair(perpX * offset, perpY * offset))
                    }
                }

                // Fire from each gun position
                for ((offsetX, offsetY) in gunOffsets) {
                    val gunX = playerX + offsetX
                    val gunY = playerY + offsetY

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
                                bullets.add(Bullet(gunX, gunY, vx, vy, true))
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
                            bullets.add(Bullet(gunX, gunY, vx, vy, true))
                        }
                    }
                }

                var actualFireRate = if (hasRapidFire) fireRate * 1.5f else fireRate
                // Overclocker - 2x fire rate for 5 seconds
                if (overclockActive) {
                    actualFireRate *= 2f
                }
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
                    val powerupName = when (p.type) {
                        PowerUpType.SPREAD_SHOT -> { hasSpreadShot = true; "SPREAD SHOT" }
                        PowerUpType.RAPID_FIRE -> { hasRapidFire = true; "RAPID FIRE" }
                        PowerUpType.PIERCING -> { hasPiercing = true; "PIERCING" }
                        PowerUpType.HOMING -> { hasHoming = true; "HOMING" }
                        PowerUpType.SPEED_BOOST -> { speedBoostStacks++; "SPEED BOOST" }
                        PowerUpType.GIANT_BULLETS -> { hasGiantBullets = true; "GIANT BULLETS" }
                        PowerUpType.BOUNCY_SHOTS -> { hasBouncyShots = true; "BOUNCY SHOTS" }
                        PowerUpType.EXPLOSIVE_ROUNDS -> { hasExplosiveRounds = true; "EXPLOSIVE ROUNDS" }
                        PowerUpType.VAMPIRE -> { vampireStacks++; "VAMPIRE" }
                        PowerUpType.SHIELD -> { shieldCount++; "SHIELD" }
                        PowerUpType.MAGNET -> {
                            hasMagnet = true
                            // Collect all orbs on the map instantly
                            val orbsCollected = greenOrbs.size
                            orbCurrency += orbsCollected
                            greenOrbs.clear()
                            "MAGNET"
                        }
                        PowerUpType.BULLET_TIME -> { hasBulletTime = true; "BULLET TIME" }
                        PowerUpType.ORBITAL -> { hasOrbital = true; "ORBITAL" }
                        PowerUpType.LASER_SIGHT -> { hasLaserSight = true; "LASER SIGHT" }
                        PowerUpType.MULTISHOT -> { multishotStacks++; "MULTISHOT" }
                        PowerUpType.SHOCKWAVE -> { hasShockwave = true; "SHOCKWAVE" }
                    }
                    // QoL: Trigger pickup feedback
                    powerupPickupFeedback = powerupName
                    powerupFeedbackTime = 1.5f
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
            putInt("clicker_barrier_level", clickerBarrierLevel)
            putInt("barrier_shield_layers", barrierShieldLayers)
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
            clickerBarrierLevel = prefs.getInt("clicker_barrier_level", 0)
            barrierShieldLayers = prefs.getInt("barrier_shield_layers", 0)

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
        CrashLogger.log("openGacha() called - Wave $wave")
        inGacha = true

        // QoL: Trigger wave completion feedback
        completedWaveNumber = wave
        waveCompleteFeedbackTime = 2f  // Show for 2 seconds
        CrashLogger.log("Wave complete feedback set for wave $completedWaveNumber")

        // Disable buttons for 1 second to prevent accidental clicks
        gachaButtonsDisabled = true
        gachaButtonEnableTimer = 1f

        // Reset joystick state to prevent auto-run after unpause
        joyPointerId = -1
        joyActive = false
        joyDx = 0f
        joyDy = 0f

        upgradeOptions.clear()

        // Weighted randomized drop table
        val dropTable = listOf(
            UpgradeOption(UpgradeType.STASIS_CORE, "Stasis Core", "Player bullets slow enemy bullets", weight = 100),
            UpgradeOption(UpgradeType.OVERCLOCKER, "Overclocker", "2x fire rate for 5 seconds", weight = 120),
            UpgradeOption(UpgradeType.QUANTUM_MIRROR, "Quantum Mirror", "Reflects bullets that graze you", weight = 80),
            UpgradeOption(UpgradeType.FRAGMENT_DRIVE, "Fragment Drive", "Kills spawn 4 micro-projectiles", weight = 100),
            UpgradeOption(UpgradeType.MULTI_GUN, "Multi-Gun", "Adds an additional parallel gun", weight = 40),  // Rare
            UpgradeOption(UpgradeType.CONVERT_TO_ORBS, "Orb Converter", "Convert all bullets to orbs", weight = 90),
            UpgradeOption(UpgradeType.CONVERT_TO_BULLETS, "Bullet Converter", "Convert all bullets to player bullets", weight = 110),
            UpgradeOption(UpgradeType.CONVERT_TO_HOMING, "Homing Converter", "Convert all bullets to homing shards", weight = 85)
        )

        // Weighted random selection - pick 3 unique options
        val totalWeight = dropTable.sumOf { it.weight }
        val chosen = mutableSetOf<UpgradeOption>()

        while (chosen.size < 3 && chosen.size < dropTable.size) {
            var random = (Math.random() * totalWeight).toInt()
            for (option in dropTable) {
                random -= option.weight
                if (random <= 0 && !chosen.contains(option)) {
                    chosen.add(option)
                    break
                }
            }
        }

        upgradeOptions.addAll(chosen)
    }

    private fun applyUpgrade(option: UpgradeOption) {
        CrashLogger.log("Applying upgrade: ${option.label} (${option.type})")
        when (option.type) {
            UpgradeType.STASIS_CORE -> {
                hasStasisCore = true
                CrashLogger.log("Stasis Core activated")
            }
            UpgradeType.OVERCLOCKER -> {
                overclockActive = true
                overclockTimer = 5f  // 5 seconds
                CrashLogger.log("Overclocker activated")
            }
            UpgradeType.QUANTUM_MIRROR -> {
                hasQuantumMirror = true
                CrashLogger.log("Quantum Mirror activated")
            }
            UpgradeType.FRAGMENT_DRIVE -> {
                hasFragmentDrive = true
                CrashLogger.log("Fragment Drive activated")
            }
            UpgradeType.MULTI_GUN -> {
                gunCount++
                CrashLogger.log("Multi-Gun activated! Gun count: $gunCount")
            }
            UpgradeType.CONVERT_TO_ORBS -> {
                CrashLogger.log("Converting bullets to orbs. Bullet count: ${bullets.size}")
                convertAllBulletsToOrbs()
            }
            UpgradeType.CONVERT_TO_BULLETS -> {
                CrashLogger.log("Converting bullets to player bullets. Bullet count: ${bullets.size}")
                convertAllBulletsToPlayerBullets()
            }
            UpgradeType.CONVERT_TO_HOMING -> {
                CrashLogger.log("Converting bullets to homing. Bullet count: ${bullets.size}, Enemy count: ${enemies.size}")
                convertAllBulletsToHoming()
            }
        }
        wave += 1
        CrashLogger.log("Wave incremented to $wave, spawning new wave")
        spawnWave()
        inGacha = false

        // Start countdown like settings menu
        countdownValue = 3
        countdownAlpha = 1f
        CrashLogger.log("Upgrade applied successfully, countdown started")
    }

    // Bullet converter functions
    private fun convertAllBulletsToOrbs() {
        val enemyBullets = bullets.filter { !it.isPlayerBullet }
        for (eb in enemyBullets) {
            greenOrbs.add(GreenOrb(eb.x, eb.y))
        }
        bullets.removeAll { !it.isPlayerBullet }
    }

    private fun convertAllBulletsToPlayerBullets() {
        // Convert all enemy bullets to player bullets shooting upward
        for (b in bullets) {
            if (!b.isPlayerBullet) {
                b.isPlayerBullet = true
                b.vx = 0f
                b.vy = -800f  // Shoot upward
            }
        }
    }

    private fun convertAllBulletsToHoming() {
        val enemyBullets = bullets.filter { !it.isPlayerBullet }.toList()
        bullets.removeAll { !it.isPlayerBullet }

        for (eb in enemyBullets) {
            // Find nearest enemy and shoot homing projectile at them
            var nearestEnemy: Enemy? = null
            var nearestDist = Float.MAX_VALUE
            for (e in enemies) {
                val dist = hypot((e.x - eb.x).toDouble(), (e.y - eb.y).toDouble()).toFloat()
                if (dist < nearestDist) {
                    nearestDist = dist
                    nearestEnemy = e
                }
            }
            if (nearestEnemy != null) {
                val dx = nearestEnemy.x - eb.x
                val dy = nearestEnemy.y - eb.y
                val len = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                val speed = 600f
                bullets.add(Bullet(eb.x, eb.y, (dx / len) * speed, (dy / len) * speed, isPlayerBullet = true))
            }
        }
    }

    // Get ROYGBV color for barrier level
    private fun getBarrierColor(level: Int): Int {
        return when (level) {
            0 -> Color.GRAY  // No upgrade
            1 -> Color.RED
            2 -> Color.argb(255, 255, 165, 0)  // Orange
            3 -> Color.YELLOW
            4 -> Color.GREEN
            5 -> Color.BLUE
            6 -> Color.argb(255, 138, 43, 226)  // Violet
            else -> Color.MAGENTA  // Beyond max
        }
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

        // Handle menu touches
        if (action == MotionEvent.ACTION_UP) {
            val x = event.getX(index)
            val y = event.getY(index)

            // Check Settings tab (always visible)
            if (!inGacha && !isDying && !inDeathScreen && settingsTabRect.contains(x, y)) {
                if (currentMenu == "SETTINGS") {
                    // Close menu and start countdown
                    currentMenu = null
                    unpauseCountdown = 3
                    unpauseCountdownAlpha = 1f
                } else {
                    currentMenu = "SETTINGS"
                }
                return true
            }

            // Check Upgrades tab (always visible)
            if (!inGacha && !isDying && !inDeathScreen && upgradesTabRect.contains(x, y)) {
                if (currentMenu == "UPGRADES") {
                    // Close menu and start countdown
                    currentMenu = null
                    unpauseCountdown = 3
                    unpauseCountdownAlpha = 1f
                } else {
                    currentMenu = "UPGRADES"
                }
                return true
            }

            // Check clicker upgrade buttons inside Upgrades menu
            if (currentMenu == "UPGRADES" && !inGacha && !isDying && !inDeathScreen) {
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
                    barrierButtonRect.contains(x, y) && orbCurrency >= cost -> {
                        orbCurrency -= cost
                        clickerBarrierLevel++
                        // Add a new barrier layer immediately when purchased (capped at 7)
                        barrierShieldLayers = min(clickerBarrierLevel, 7)
                        return true
                    }
                }
            }

            // Check settings controls (in Settings menu)
            if (currentMenu == "SETTINGS" && !inGacha && !isDying && !inDeathScreen) {
                when {
                    musicToggleRect.contains(x, y) -> {
                        val musicEnabled = settingsPrefs.getBoolean("music_enabled", true)
                        settingsPrefs.edit().putBoolean("music_enabled", !musicEnabled).apply()
                        if (!musicEnabled) {
                            AudioManager.startMusic(context)
                        } else {
                            AudioManager.stopMusic()
                        }
                        return true
                    }
                    soundToggleRect.contains(x, y) -> {
                        val soundEnabled = settingsPrefs.getBoolean("sound_enabled", true)
                        settingsPrefs.edit().putBoolean("sound_enabled", !soundEnabled).apply()
                        if (!soundEnabled) {
                            AudioManager.startRain(context)
                        } else {
                            AudioManager.stopRain()
                        }
                        return true
                    }
                    powerupsToggleRect.contains(x, y) -> {
                        val powerupsEnabled = settingsPrefs.getBoolean("powerups_enabled", true)
                        settingsPrefs.edit().putBoolean("powerups_enabled", !powerupsEnabled).apply()
                        return true
                    }
                    saveButtonRect.contains(x, y) -> {
                        prefs.edit().putBoolean("trigger_save", true).apply()
                        saveGameState()
                        return true
                    }
                    loadButtonRect.contains(x, y) -> {
                        if (prefs.getInt("saved_wave", 0) > 0) {
                            prefs.edit().putBoolean("trigger_load", true).apply()
                            loadGameState()
                        }
                        return true
                    }
                }
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

                // Don't activate joystick if touching UI buttons or menus
                val touchingUI = !inGacha && !isDying && !inDeathScreen && (
                    settingsTabRect.contains(x, y) ||
                    upgradesTabRect.contains(x, y) ||
                    (currentMenu == "UPGRADES" && (
                        damageButtonRect.contains(x, y) ||
                        fireButtonRect.contains(x, y) ||
                        speedButtonRect.contains(x, y) ||
                        hpButtonRect.contains(x, y) ||
                        barrierButtonRect.contains(x, y)
                    )) ||
                    (currentMenu == "SETTINGS" && (
                        musicToggleRect.contains(x, y) ||
                        soundToggleRect.contains(x, y) ||
                        powerupsToggleRect.contains(x, y) ||
                        musicVolumeSliderRect.contains(x, y) ||
                        rainVolumeSliderRect.contains(x, y) ||
                        saveButtonRect.contains(x, y) ||
                        loadButtonRect.contains(x, y)
                    ))
                )

                // Handle slider dragging start
                if (currentMenu == "SETTINGS") {
                    if (musicVolumeSliderRect.contains(x, y)) {
                        draggingMusicSlider = true
                        val ratio = ((x - musicVolumeSliderRect.left) / musicVolumeSliderRect.width()).coerceIn(0f, 1f)
                        settingsPrefs.edit().putFloat("music_volume", ratio).apply()
                        AudioManager.updateMusicVolume(ratio)
                        return true
                    }
                    if (rainVolumeSliderRect.contains(x, y)) {
                        draggingRainSlider = true
                        val ratio = ((x - rainVolumeSliderRect.left) / rainVolumeSliderRect.width()).coerceIn(0f, 1f)
                        settingsPrefs.edit().putFloat("rain_volume", ratio).apply()
                        AudioManager.rainVolume = ratio
                        return true
                    }
                }

                // Floating joystick: appears anywhere on screen (except UI buttons or when menus are open)
                if (joyPointerId == -1 && !touchingUI && !inGacha && !inDeathScreen && currentMenu == null) {
                    joyPointerId = event.getPointerId(index)
                    joyBaseX = x
                    joyBaseY = y
                    joyDx = 0f
                    joyDy = 0f
                    joyActive = true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val x = event.getX(0)

                // Handle slider dragging
                if (draggingMusicSlider && musicVolumeSliderRect.width() > 0) {
                    val ratio = ((x - musicVolumeSliderRect.left) / musicVolumeSliderRect.width()).coerceIn(0f, 1f)
                    settingsPrefs.edit().putFloat("music_volume", ratio).apply()
                    AudioManager.updateMusicVolume(ratio)
                    return true
                }
                if (draggingRainSlider && rainVolumeSliderRect.width() > 0) {
                    val ratio = ((x - rainVolumeSliderRect.left) / rainVolumeSliderRect.width()).coerceIn(0f, 1f)
                    settingsPrefs.edit().putFloat("rain_volume", ratio).apply()
                    AudioManager.rainVolume = ratio
                    return true
                }

                if (joyPointerId != -1) {
                    val pIndex = event.findPointerIndex(joyPointerId)
                    if (pIndex != -1) {
                        val jx = event.getX(pIndex)
                        val jy = event.getY(pIndex)
                        joyDx = jx - joyBaseX
                        joyDy = jy - joyBaseY
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
                // Release slider dragging
                draggingMusicSlider = false
                draggingRainSlider = false

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
                // Player bullets - cyan diamonds with glow (reuse path)
                canvas.drawCircle(b.x, b.y, 15f, bulletGlowPaint)
                bulletPath.reset()
                bulletPath.moveTo(b.x, b.y - 8f)
                bulletPath.lineTo(b.x + 6f, b.y)
                bulletPath.lineTo(b.x, b.y + 8f)
                bulletPath.lineTo(b.x - 6f, b.y)
                bulletPath.close()
                canvas.drawPath(bulletPath, bulletPaint)
            } else {
                // Enemy bullets - red circles with glow
                canvas.drawCircle(b.x, b.y, 10f, enemyBulletGlowPaint)
                canvas.drawCircle(b.x, b.y, 5f, enemyBulletPaint)
            }
        }

        // Draw enemies with animated sprites using individual sprite files
        for (e in enemies) {
            // Check if this is a boss - render differently
            if (e.isBoss) {
                val bossBitmap = bossSprites[e.type]
                if (bossBitmap != null) {
                    // Draw boss much larger
                    val bossSize = 120f  // 2x larger than regular enemies

                    // Draw massive glow behind boss
                    val glowRadius = e.getGlowRadius() * 2f
                    canvas.drawCircle(e.x, e.y, bossSize / 2f + glowRadius, enemyGlowPaint)

                    // Draw HP bar for boss (using class-level paints)
                    val barWidth = 150f
                    val barHeight = 12f
                    val barX = e.x - barWidth / 2f
                    val barY = e.y - bossSize / 2f - 30f

                    canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, bossHpBarBgPaint)
                    val hpRatio = (e.hp / e.maxHp).coerceIn(0f, 1f)
                    canvas.drawRect(barX, barY, barX + barWidth * hpRatio, barY + barHeight, bossHpBarFillPaint)

                    // Determine if boss should be flipped based on player position
                    val dx = playerX - e.x
                    val flipHorizontal = dx > 0  // Flip when player is to the right

                    // Draw boss sprite with directional flipping
                    if (flipHorizontal) {
                        canvas.save()
                        canvas.scale(-1f, 1f, e.x, e.y)
                    }

                    val dstRect = RectF(
                        e.x - bossSize / 2f,
                        e.y - bossSize / 2f,
                        e.x + bossSize / 2f,
                        e.y + bossSize / 2f
                    )
                    canvas.drawBitmap(bossBitmap, null, dstRect, spritePaint)

                    if (flipHorizontal) {
                        canvas.restore()
                    }
                }
                continue  // Skip normal enemy rendering
            }

            // Row 1 character (Blue-hooded) - CIRCLE
            //   moving down = 1,4,7
            //   moving left = 2,5,8
            //   moving up = 3,6,9
            // Row 2 character (Pink-haired) - TRIANGLE
            //   moving down = 10,13,16
            //   moving left = 11,14,17
            //   moving up = 12,15,18
            // Row 3 character (Brown-hooded) - SQUARE/PENTAGON/HEXAGON
            //   moving down = 19,22,25
            //   moving left = 20,23,26
            //   moving up = 21,24,27

            // Determine direction based on movement toward player
            val dx = playerX - e.x
            val dy = playerY - e.y
            val absX = abs(dx)
            val absY = abs(dy)

            // Get the sprite number sequence for this enemy type and direction
            val spriteSequence = when (e.type) {
                EnemyType.CIRCLE -> when {
                    absY > absX && dy > 0 -> intArrayOf(1, 4, 7)    // Down
                    absY > absX && dy < 0 -> intArrayOf(3, 6, 9)    // Up
                    absX >= absY && dx < 0 -> intArrayOf(2, 5, 8)   // Left
                    else -> intArrayOf(2, 5, 8)                      // Right (flip left)
                }
                EnemyType.TRIANGLE -> when {
                    absY > absX && dy > 0 -> intArrayOf(10, 13, 16)  // Down
                    absY > absX && dy < 0 -> intArrayOf(12, 15, 18)  // Up
                    absX >= absY && dx < 0 -> intArrayOf(11, 14, 17) // Left
                    else -> intArrayOf(11, 14, 17)                    // Right (flip left)
                }
                else -> when {  // SQUARE, PENTAGON, HEXAGON
                    absY > absX && dy > 0 -> intArrayOf(19, 22, 25)  // Down
                    absY > absX && dy < 0 -> intArrayOf(21, 24, 27)  // Up
                    absX >= absY && dx < 0 -> intArrayOf(20, 23, 26) // Left
                    else -> intArrayOf(20, 23, 26)                    // Right (flip left)
                }
            }

            // Determine if we need to flip horizontally (moving right)
            val flipHorizontal = absX >= absY && dx > 0

            // Get current animation frame (cycle through 3 frames)
            val animIndex = e.animFrame % 3
            val spriteNumber = spriteSequence[animIndex]

            // Get the sprite bitmap
            val spriteBitmap = enemySprites[spriteNumber]
            if (spriteBitmap != null) {
                // Draw sprite (make it big enough to see clearly)
                val enemySize = 60f

                // Draw glow BEHIND sprite
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
                canvas.drawBitmap(spriteBitmap, null, dstRect, spritePaint)

                if (flipHorizontal) {
                    canvas.restore()
                }
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

        // Draw walls (pink obstacles)
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
        val isHorizontalStrip: Boolean
        when {
            isDying -> {
                spriteSheet = playerDeathSprite
                totalFrames = 5 // Death animation frames
                isHorizontalStrip = false // Vertical strip
            }
            isPlayingHitAnimation -> {
                spriteSheet = playerHitSprite
                totalFrames = 2 // Hit animation frames (player_attack.png used as hit)
                isHorizontalStrip = false // Vertical strip
            }
            isMoving -> {
                // Character sprites are horizontal strips
                spriteSheet = playerRunSprite
                totalFrames = 6
                isHorizontalStrip = true
            }
            else -> {
                // Character sprites are horizontal strips
                spriteSheet = playerIdleSprite
                totalFrames = 4
                isHorizontalStrip = true
            }
        }

        // Calculate frame size based on sprite orientation
        val frameWidth: Int
        val frameHeight: Int
        if (isHorizontalStrip) {
            // Horizontal strip: width divided by frames, height is full
            frameWidth = if (totalFrames > 0) spriteSheet.width / totalFrames else spriteSheet.width
            frameHeight = spriteSheet.height
        } else {
            // Vertical strip: width is full, height divided by frames
            frameWidth = spriteSheet.width
            frameHeight = if (totalFrames > 0) spriteSheet.height / totalFrames else spriteSheet.height
        }

        // Source rect for current frame
        val srcRect = if (isHorizontalStrip) {
            Rect(
                currentFrame * frameWidth,
                0,
                (currentFrame + 1) * frameWidth,
                frameHeight
            )
        } else {
            Rect(
                0,
                currentFrame * frameHeight,
                frameWidth,
                (currentFrame + 1) * frameHeight
            )
        }

        // Destination rect (where to draw on screen)
        // Use actual frame dimensions scaled to match old sprite size
        val actualSpriteWidth: Float
        val actualSpriteHeight: Float

        if (isHorizontalStrip) {
            // Character sprites: scale based on actual frame size
            // Slightly larger size (140px height) for better visibility
            val targetHeight = 140f
            val aspectRatio = if (frameHeight > 0) frameWidth.toFloat() / frameHeight.toFloat() else 1f
            actualSpriteHeight = targetHeight
            actualSpriteWidth = targetHeight * aspectRatio
        } else {
            // Old sprites: use hardcoded values
            actualSpriteWidth = spriteWidth
            actualSpriteHeight = spriteHeight
        }

        // Adjust position for character sprites to account for sprite padding/offset
        val offsetX = if (isHorizontalStrip) 20f else 0f  // Shift right (sprite content is left of center)
        val offsetY = if (isHorizontalStrip) -20f else 0f  // Shift up (sprite content is below center)

        val dstRect = RectF(
            playerX - actualSpriteWidth / 2f + offsetX,
            playerY - actualSpriteHeight / 2f + offsetY,
            playerX + actualSpriteWidth / 2f + offsetX,
            playerY + actualSpriteHeight / 2f + offsetY
        )

        // Draw Prismatic Barrier (wavy ROYGBV outlines)
        if (barrierShieldLayers > 0) {
            for (layer in 0 until barrierShieldLayers) {
                // Each layer has progressively larger radius
                val baseRadius = actualSpriteHeight / 2f + 20f
                val layerRadius = baseRadius + layer * 15f

                // Get color for this layer (cycles through ROYGBV)
                val layerColor = when (layer % 7) {
                    0 -> Color.RED
                    1 -> Color.argb(255, 255, 165, 0)  // Orange
                    2 -> Color.YELLOW
                    3 -> Color.GREEN
                    4 -> Color.CYAN
                    5 -> Color.BLUE
                    else -> Color.argb(255, 138, 43, 226)  // Violet
                }

                // Create wavy path for barrier (reuse path object)
                barrierPath.reset()
                val segments = 36  // Number of points for smooth circle
                for (i in 0..segments) {
                    val angle = (i / segments.toFloat()) * 2f * Math.PI.toFloat()
                    // Add wave effect with different phase per layer
                    val wavePhase = barrierWaveOffset + layer * 0.5f
                    val wave = sin((angle * 4f + wavePhase).toDouble()).toFloat() * 8f
                    val radius = layerRadius + wave
                    val x = playerX + cos(angle.toDouble()).toFloat() * radius
                    val y = playerY + sin(angle.toDouble()).toFloat() * radius

                    if (i == 0) {
                        barrierPath.moveTo(x, y)
                    } else {
                        barrierPath.lineTo(x, y)
                    }
                }
                barrierPath.close()

                // Draw the wavy barrier outline (reuse paint, just change color)
                barrierPaint.color = layerColor
                canvas.drawPath(barrierPath, barrierPaint)

                // Add inner glow (reuse paint, just change color)
                barrierGlowPaint.color = layerColor
                canvas.drawPath(barrierPath, barrierGlowPaint)
            }
        }

        // Draw neon glow behind sprite (smaller radius)
        canvas.drawCircle(playerX, playerY, actualSpriteHeight / 2.5f, spriteGlowPaint)

        // Draw sprite with proper flipping for horizontal character sprites
        try {
            if (isHorizontalStrip && playerFacingLeft) {
                // Flip sprite horizontally by scaling canvas
                canvas.save()
                // Translate to player position, flip, then translate back
                canvas.translate(playerX, playerY)
                canvas.scale(-1f, 1f)
                canvas.translate(-playerX, -playerY)
                canvas.drawBitmap(spriteSheet, srcRect, dstRect, spritePaint)
                canvas.restore()
            } else {
                canvas.drawBitmap(spriteSheet, srcRect, dstRect, spritePaint)
            }
        } catch (e: Exception) {
            addDebugLog("Sprite draw error: ${e.message}, frames=$totalFrames, w=$frameWidth, h=$frameHeight")
        }

        // Restore canvas from camera and screen shake
        canvas.restore()

        // Draw UI elements in screen space (not world space)

        // HP bar (bigger and easier to see)
        val barWidth = w * 0.6f
        val barHeight = 36f  // Increased from 24f
        val barX = (w - barWidth) / 2f
        val barY = 50f  // Moved down slightly
        canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, hpBgPaint)
        val hpRatio = (playerHp.toFloat() / maxHp.toFloat()).coerceIn(0f, 1f)
        canvas.drawRect(barX, barY, barX + barWidth * hpRatio, barY + barHeight, hpFillPaint)

        // Wave counter (bigger text)
        val waveTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 48f  // Increased
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }
        canvas.drawText("Wave $wave", 30f, barY + barHeight + 55f, waveTextPaint)

        // QoL: Wave completion feedback (big centered text)
        if (waveCompleteFeedbackTime > 0f) {
            val alpha = (waveCompleteFeedbackTime * 255f).toInt().coerceIn(0, 255)
            val scale = if (waveCompleteFeedbackTime > 1.5f) {
                // First 0.5s: scale up from 0.5 to 1.0
                0.5f + (2f - waveCompleteFeedbackTime) * 1f
            } else {
                1f
            }
            val waveCompletePaint = Paint().apply {
                color = Color.argb(alpha, 0, 255, 255)
                textSize = 80f * scale
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
                setShadowLayer(20f, 0f, 0f, Color.argb(alpha, 0, 255, 255))
            }
            canvas.drawText("WAVE $completedWaveNumber COMPLETE!", w / 2f, h / 3f, waveCompletePaint)
        }

        // QoL: Powerup pickup feedback (top-center)
        if (powerupFeedbackTime > 0f) {
            val alpha = (powerupFeedbackTime * 255f).toInt().coerceIn(0, 255)
            val yOffset = (1.5f - powerupFeedbackTime) * 30f  // Floats upward
            val pickupPaint = Paint().apply {
                color = Color.argb(alpha, 255, 215, 0)  // Gold color
                textSize = 40f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
                setShadowLayer(15f, 0f, 0f, Color.argb(alpha, 255, 215, 0))
            }
            canvas.drawText("+ $powerupPickupFeedback", w / 2f, 150f - yOffset, pickupPaint)
        }

        // QoL: Enemy proximity warning (pulsing red border)
        if (enemyProximityWarning && !inGacha) {
            val pulseAlpha = ((sin(System.currentTimeMillis() / 200.0) + 1.0) / 2.0 * 100).toInt()
            val warningPaint = Paint().apply {
                color = Color.argb(pulseAlpha, 255, 0, 0)
                style = Paint.Style.STROKE
                strokeWidth = 10f
            }
            canvas.drawRect(5f, 5f, w - 5f, h - 5f, warningPaint)
        }

        // Draw flyout menus (slides in from right)
        if (menuSlideProgress > 0.01f) {
            val menuWidth = w * 0.85f
            val menuLeft = w - (menuWidth * menuSlideProgress)

            val menuBgPaint = Paint().apply {
                color = Color.argb(240, 10, 10, 25)
                isAntiAlias = true
            }

            canvas.drawRect(menuLeft, 0f, w, h, menuBgPaint)

            val menuTitlePaint = Paint().apply {
                color = Color.CYAN
                textSize = 56f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
                setShadowLayer(8f, 2f, 2f, Color.BLACK)
            }

            // Draw appropriate menu based on currentMenu
            when (currentMenu) {
                "SETTINGS" -> {
                    // Settings menu with all controls
                    canvas.drawText("SETTINGS", menuLeft + menuWidth / 2f, 100f, menuTitlePaint)

                    // Active Upgrades Display Section
                    val upgradeTitlePaint = Paint().apply {
                        color = Color.CYAN
                        textSize = 36f
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        isAntiAlias = true
                    }
                    val upgradeTextPaint = Paint().apply {
                        color = Color.argb(255, 100, 255, 150)
                        textSize = 24f
                        textAlign = Paint.Align.LEFT
                        isAntiAlias = true
                    }
                    val upgradeBoxPaint = Paint().apply {
                        color = Color.argb(100, 0, 100, 100)
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    val upgradeBoxBorderPaint = Paint().apply {
                        color = Color.CYAN
                        style = Paint.Style.STROKE
                        strokeWidth = 3f
                        isAntiAlias = true
                    }

                    var upgradeYPos = 140f
                    val upgradeBoxLeft = menuLeft + 20f
                    val upgradeBoxRight = menuLeft + menuWidth - 20f

                    // Collect active upgrades
                    val activeUpgrades = mutableListOf<String>()
                    if (hasStasisCore) activeUpgrades.add("Stasis Core - Bullets slow enemies")
                    if (overclockActive) activeUpgrades.add("Overclocker - 2x fire rate (${overclockTimer.toInt()}s)")
                    else if (overclockTimer > 0f && !overclockActive) activeUpgrades.add("Overclocker (ready)")
                    if (hasQuantumMirror) activeUpgrades.add("Quantum Mirror - Graze reflect")
                    if (hasFragmentDrive) activeUpgrades.add("Fragment Drive - Kill fragments")

                    if (activeUpgrades.isNotEmpty()) {
                        canvas.drawText("ACTIVE UPGRADES", menuLeft + menuWidth / 2f, upgradeYPos, upgradeTitlePaint)
                        upgradeYPos += 15f

                        for (upgrade in activeUpgrades) {
                            val boxHeight = 40f
                            val upgradeBox = RectF(upgradeBoxLeft, upgradeYPos, upgradeBoxRight, upgradeYPos + boxHeight)
                            canvas.drawRoundRect(upgradeBox, 8f, 8f, upgradeBoxPaint)
                            canvas.drawRoundRect(upgradeBox, 8f, 8f, upgradeBoxBorderPaint)
                            canvas.drawText(upgrade, upgradeBoxLeft + 10f, upgradeYPos + 27f, upgradeTextPaint)
                            upgradeYPos += boxHeight + 8f
                        }
                        upgradeYPos += 20f
                    } else {
                        canvas.drawText("ACTIVE UPGRADES", menuLeft + menuWidth / 2f, upgradeYPos, upgradeTitlePaint)
                        upgradeYPos += 15f
                        upgradeTextPaint.textAlign = Paint.Align.CENTER
                        canvas.drawText("(None - beat waves for upgrades!)", menuLeft + menuWidth / 2f, upgradeYPos + 20f, upgradeTextPaint)
                        upgradeTextPaint.textAlign = Paint.Align.LEFT
                        upgradeYPos += 60f
                    }

                    // Stat Tracker Table
                    canvas.drawText("STATS", menuLeft + menuWidth / 2f, upgradeYPos, upgradeTitlePaint)
                    upgradeYPos += 10f

                    val statTablePaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 20f
                        isAntiAlias = true
                    }
                    val statHeaderPaint = Paint().apply {
                        color = Color.CYAN
                        textSize = 20f
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        isAntiAlias = true
                    }
                    val statValuePaint = Paint().apply {
                        color = Color.argb(255, 100, 255, 150)
                        textSize = 20f
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                        isAntiAlias = true
                    }

                    // Calculate all stat modifiers
                    val damageBase = clickerDamageLevel.toFloat()
                    val damageModifier = 0f // No current modifiers for damage
                    val damageTotal = damageBase + damageModifier

                    val fireRateBase = clickerFireRateLevel * 0.25f + 1f
                    val fireRateModifier = (if (hasRapidFire) fireRateBase * 0.5f else 0f) +
                                          (if (overclockActive) fireRateBase else 0f)
                    val fireRateTotal = fireRateBase + fireRateModifier
                    val fireRatePercent = if (fireRateBase > 0) ((fireRateModifier / fireRateBase) * 100).toInt() else 0

                    val speedBase = 250f
                    val speedModifier = speedBoostStacks * 0.10f * speedBase
                    val speedTotal = speedBase + speedModifier
                    val speedPercent = if (speedBase > 0) ((speedModifier / speedBase) * 100).toInt() else 0

                    val hpBase = clickerHpLevel * 50f + 100f
                    val hpModifier = 0f // No current modifiers for HP
                    val hpTotal = hpBase + hpModifier

                    val multishotBase = 1
                    val multishotModifier = multishotStacks
                    val multishotTotal = multishotBase + multishotModifier
                    val multishotPercent = if (multishotBase > 0) ((multishotModifier.toFloat() / multishotBase) * 100).toInt() else 0

                    val barrierLayers = barrierShieldLayers
                    val barrierMax = clickerBarrierLevel

                    // Draw table header
                    val col1X = menuLeft + 30f
                    val col2X = menuLeft + menuWidth * 0.35f
                    val col3X = menuLeft + menuWidth * 0.58f
                    val col4X = menuLeft + menuWidth * 0.80f
                    val rowHeight = 28f

                    canvas.drawText("STAT", col1X, upgradeYPos, statHeaderPaint)
                    canvas.drawText("BASE", col2X, upgradeYPos, statHeaderPaint)
                    canvas.drawText("MOD", col3X, upgradeYPos, statHeaderPaint)
                    canvas.drawText("TOTAL", col4X, upgradeYPos, statHeaderPaint)
                    upgradeYPos += rowHeight

                    // Draw table rows
                    fun drawStatRow(name: String, base: String, mod: String, total: String) {
                        canvas.drawText(name, col1X, upgradeYPos, statTablePaint)
                        canvas.drawText(base, col2X, upgradeYPos, statValuePaint)
                        canvas.drawText(mod, col3X, upgradeYPos, statValuePaint)
                        canvas.drawText(total, col4X, upgradeYPos, statValuePaint)
                        upgradeYPos += rowHeight
                    }

                    drawStatRow("Damage", damageBase.toInt().toString(),
                                if (damageModifier > 0) "+${damageModifier.toInt()}" else "-",
                                damageTotal.toInt().toString())

                    drawStatRow("Fire Rate", String.format("%.2f", fireRateBase),
                                if (fireRatePercent > 0) "+$fireRatePercent%" else "-",
                                String.format("%.2f", fireRateTotal))

                    drawStatRow("Speed", speedBase.toInt().toString(),
                                if (speedPercent > 0) "+$speedPercent%" else "-",
                                speedTotal.toInt().toString())

                    drawStatRow("Max HP", hpBase.toInt().toString(),
                                if (hpModifier > 0) "+${hpModifier.toInt()}" else "-",
                                hpTotal.toInt().toString())

                    drawStatRow("Multishot", multishotBase.toString(),
                                if (multishotPercent > 0) "+$multishotPercent%" else "-",
                                multishotTotal.toString())

                    drawStatRow("Barriers", "$barrierLayers/$barrierMax", "-", "$barrierLayers/$barrierMax")

                    upgradeYPos += 15f

                    val labelPaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 32f
                        isAntiAlias = true
                    }
                    val togglePaint = Paint().apply {
                        color = Color.CYAN
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    val toggleOffPaint = Paint().apply {
                        color = Color.argb(100, 100, 100, 100)
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    val toggleBorderPaint = Paint().apply {
                        color = Color.CYAN
                        style = Paint.Style.STROKE
                        strokeWidth = 4f
                        isAntiAlias = true
                    }
                    val sliderTrackPaint = Paint().apply {
                        color = Color.DKGRAY
                        style = Paint.Style.FILL
                    }

                    val padding = 30f
                    var yPos = upgradeYPos  // Start after upgrades display
                    val toggleWidth = 120f
                    val toggleHeight = 50f

                    // Music toggle
                    val musicEnabled = settingsPrefs.getBoolean("music_enabled", true)
                    canvas.drawText("MUSIC", menuLeft + padding, yPos + 36f, labelPaint)
                    musicToggleRect = RectF(
                        menuLeft + menuWidth - toggleWidth - padding,
                        yPos,
                        menuLeft + menuWidth - padding,
                        yPos + toggleHeight
                    )
                    canvas.drawRoundRect(musicToggleRect, 25f, 25f, if (musicEnabled) togglePaint else toggleOffPaint)
                    canvas.drawRoundRect(musicToggleRect, 25f, 25f, toggleBorderPaint)
                    labelPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText(if (musicEnabled) "ON" else "OFF", musicToggleRect.centerX(), musicToggleRect.centerY() + 10f, labelPaint)
                    labelPaint.textAlign = Paint.Align.LEFT
                    yPos += 70f

                    // Music volume slider
                    val musicVolume = settingsPrefs.getFloat("music_volume", 0.5f)
                    canvas.drawText("Music Vol: ${(musicVolume * 100).toInt()}%", menuLeft + padding, yPos, labelPaint)
                    yPos += 10f
                    musicVolumeSliderRect = RectF(menuLeft + padding, yPos, menuLeft + menuWidth - padding, yPos + 40f)
                    canvas.drawRoundRect(musicVolumeSliderRect, 10f, 10f, sliderTrackPaint)
                    val musicFillWidth = musicVolumeSliderRect.width() * musicVolume
                    canvas.drawRoundRect(
                        musicVolumeSliderRect.left, musicVolumeSliderRect.top,
                        musicVolumeSliderRect.left + musicFillWidth, musicVolumeSliderRect.bottom,
                        10f, 10f, togglePaint
                    )
                    yPos += 60f

                    // Sound toggle
                    val soundEnabled = settingsPrefs.getBoolean("sound_enabled", true)
                    canvas.drawText("SOUND", menuLeft + padding, yPos + 36f, labelPaint)
                    soundToggleRect = RectF(
                        menuLeft + menuWidth - toggleWidth - padding,
                        yPos,
                        menuLeft + menuWidth - padding,
                        yPos + toggleHeight
                    )
                    canvas.drawRoundRect(soundToggleRect, 25f, 25f, if (soundEnabled) togglePaint else toggleOffPaint)
                    canvas.drawRoundRect(soundToggleRect, 25f, 25f, toggleBorderPaint)
                    labelPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText(if (soundEnabled) "ON" else "OFF", soundToggleRect.centerX(), soundToggleRect.centerY() + 10f, labelPaint)
                    labelPaint.textAlign = Paint.Align.LEFT
                    yPos += 70f

                    // Rain volume slider
                    val rainVolume = settingsPrefs.getFloat("rain_volume", 0.3f)
                    canvas.drawText("Rain Vol: ${(rainVolume * 100).toInt()}%", menuLeft + padding, yPos, labelPaint)
                    yPos += 10f
                    rainVolumeSliderRect = RectF(menuLeft + padding, yPos, menuLeft + menuWidth - padding, yPos + 40f)
                    canvas.drawRoundRect(rainVolumeSliderRect, 10f, 10f, sliderTrackPaint)
                    val rainFillWidth = rainVolumeSliderRect.width() * rainVolume
                    canvas.drawRoundRect(
                        rainVolumeSliderRect.left, rainVolumeSliderRect.top,
                        rainVolumeSliderRect.left + rainFillWidth, rainVolumeSliderRect.bottom,
                        10f, 10f, togglePaint
                    )
                    yPos += 60f

                    // Powerups toggle
                    val powerupsEnabled = settingsPrefs.getBoolean("powerups_enabled", true)
                    canvas.drawText("POWERUPS", menuLeft + padding, yPos + 36f, labelPaint)
                    powerupsToggleRect = RectF(
                        menuLeft + menuWidth - toggleWidth - padding,
                        yPos,
                        menuLeft + menuWidth - padding,
                        yPos + toggleHeight
                    )
                    canvas.drawRoundRect(powerupsToggleRect, 25f, 25f, if (powerupsEnabled) togglePaint else toggleOffPaint)
                    canvas.drawRoundRect(powerupsToggleRect, 25f, 25f, toggleBorderPaint)
                    labelPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText(if (powerupsEnabled) "ON" else "OFF", powerupsToggleRect.centerX(), powerupsToggleRect.centerY() + 10f, labelPaint)
                    labelPaint.textAlign = Paint.Align.LEFT
                    yPos += 80f

                    // Save/Load buttons
                    val buttonWidth = (menuWidth - padding * 3) / 2f
                    val buttonHeight = 80f
                    val hasSave = prefs.getInt("saved_wave", 0) > 0

                    saveButtonRect = RectF(menuLeft + padding, yPos, menuLeft + padding + buttonWidth, yPos + buttonHeight)
                    loadButtonRect = RectF(menuLeft + padding * 2 + buttonWidth, yPos, menuLeft + menuWidth - padding, yPos + buttonHeight)

                    val buttonBgPaint = Paint().apply {
                        color = Color.argb(150, 20, 20, 30)
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }

                    // Save button
                    canvas.drawRoundRect(saveButtonRect, 15f, 15f, buttonBgPaint)
                    canvas.drawRoundRect(saveButtonRect, 15f, 15f, toggleBorderPaint)
                    labelPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText("SAVE", saveButtonRect.centerX(), saveButtonRect.centerY() + 10f, labelPaint)

                    // Load button (grayed out if no save)
                    val loadBorderPaint = Paint().apply {
                        color = if (hasSave) Color.CYAN else Color.GRAY
                        style = Paint.Style.STROKE
                        strokeWidth = 4f
                        isAntiAlias = true
                    }
                    val loadTextPaint = Paint().apply {
                        color = if (hasSave) Color.WHITE else Color.GRAY
                        textSize = 32f
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    canvas.drawRoundRect(loadButtonRect, 15f, 15f, buttonBgPaint)
                    canvas.drawRoundRect(loadButtonRect, 15f, 15f, loadBorderPaint)
                    canvas.drawText("LOAD", loadButtonRect.centerX(), loadButtonRect.centerY() + 10f, loadTextPaint)

                    labelPaint.textAlign = Paint.Align.LEFT
                    yPos += buttonHeight + 30f

                    // Debug Logs Section
                    canvas.drawText("DEBUG LOGS (last ${debugLogs.size})", menuLeft + menuWidth / 2f, yPos, upgradeTitlePaint)
                    yPos += 30f

                    val debugTextPaint = Paint().apply {
                        color = Color.argb(200, 150, 150, 150)
                        textSize = 16f
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                        isAntiAlias = true
                    }

                    // Show last 10 debug logs
                    val logsToShow = debugLogs.takeLast(10)
                    for (log in logsToShow) {
                        canvas.drawText(log, menuLeft + padding, yPos, debugTextPaint)
                        yPos += 20f
                    }
                }
                "UPGRADES" -> {
                    // Upgrades menu (existing clicker UI)
                    canvas.drawText("UPGRADES", menuLeft + menuWidth / 2f, 100f, menuTitlePaint)

                    // Currency display with "Spend!" CTA
                    val currencyBigPaint = Paint().apply {
                        color = Color.argb(255, 0, 255, 100) // Green
                        textSize = 48f
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        isAntiAlias = true
                    }
                    val spendCtaPaint = Paint().apply {
                        color = Color.YELLOW
                        textSize = 32f
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        isAntiAlias = true
                    }
                    canvas.drawText("$orbCurrency Orbs", menuLeft + menuWidth / 2f, 170f, currencyBigPaint)
                    canvas.drawText("SPEND!", menuLeft + menuWidth / 2f, 210f, spendCtaPaint)

                    // Button paints - black background with green text
                    val buttonBgPaint = Paint().apply {
                        color = Color.BLACK
                        isAntiAlias = true
                    }
                    val buttonShadowPaint = Paint().apply {
                        color = Color.argb(150, 0, 0, 0)
                        isAntiAlias = true
                    }
                    val buttonBorderPaint = Paint().apply {
                        color = Color.GREEN
                        style = Paint.Style.STROKE
                        strokeWidth = 5f
                        isAntiAlias = true
                    }
                    val buttonLabelPaint = Paint().apply {
                        color = Color.GREEN
                        textSize = 42f
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        isAntiAlias = true
                    }
                    val buttonDetailPaint = Paint().apply {
                        color = Color.argb(200, 0, 255, 0) // Lighter green
                        textSize = 28f
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                        isAntiAlias = true
                    }

                    // Upgrade buttons (vertically stacked at BOTTOM of menu for thumb reach)
                    val buttonPadding = 30f  // Padding from edges
                    val buttonWidth = menuWidth - (buttonPadding * 2)
                    val buttonHeight = 100f
                    val buttonGap = 20f
                    val buttonStartX = menuLeft + buttonPadding
                    // Start from bottom and work upward
                    val bottomMargin = 30f
                    var buttonY = h - bottomMargin - buttonHeight

                    // Helper function to draw button
                    fun drawButton(rect: RectF, label: String, level: Int, cost: Int) {
                        // Shadow (offset down and right)
                        val shadowRect = RectF(rect.left + 4f, rect.top + 4f, rect.right + 4f, rect.bottom + 4f)
                        canvas.drawRoundRect(shadowRect, 12f, 12f, buttonShadowPaint)

                        // Black background
                        canvas.drawRoundRect(rect, 12f, 12f, buttonBgPaint)

                        // Green outline
                        canvas.drawRoundRect(rect, 12f, 12f, buttonBorderPaint)

                        // Green text
                        canvas.drawText(label, rect.centerX(), rect.centerY() - 20f, buttonLabelPaint)
                        canvas.drawText("Level $level", rect.centerX(), rect.centerY() + 10f, buttonDetailPaint)
                        canvas.drawText("Cost: $cost orbs", rect.centerX(), rect.centerY() + 35f, buttonDetailPaint)
                    }

                    // Button 5: HP (at bottom for easy thumb reach)
                    hpButtonRect = RectF(buttonStartX, buttonY, buttonStartX + buttonWidth, buttonY + buttonHeight)
                    drawButton(hpButtonRect, "HEALTH+", clickerHpLevel, 10)
                    buttonY -= buttonHeight + buttonGap

                    // Button 4: Prismatic Barrier (with ROYGBV color progression + AOE at 7+)
                    barrierButtonRect = RectF(buttonStartX, buttonY, buttonStartX + buttonWidth, buttonY + buttonHeight)
                    val barrierColor = getBarrierColor(clickerBarrierLevel)
                    val barrierLabel = when {
                        clickerBarrierLevel == 0 -> "P.BARRIER"
                        clickerBarrierLevel == 1 -> "P.BARRIER (Red)"
                        clickerBarrierLevel == 2 -> "P.BARRIER (Orange)"
                        clickerBarrierLevel == 3 -> "P.BARRIER (Yellow)"
                        clickerBarrierLevel == 4 -> "P.BARRIER (Green)"
                        clickerBarrierLevel == 5 -> "P.BARRIER (Blue)"
                        clickerBarrierLevel == 6 -> "P.BARRIER (Violet)"
                        clickerBarrierLevel == 7 -> "P.BARRIER (MAX)"
                        else -> "P.BARRIER +${(clickerBarrierLevel - 7) * 5} AOE"
                    }
                    // Custom draw for barrier button with colored border
                    val shadowRect = RectF(barrierButtonRect.left + 4f, barrierButtonRect.top + 4f,
                                          barrierButtonRect.right + 4f, barrierButtonRect.bottom + 4f)
                    canvas.drawRoundRect(shadowRect, 12f, 12f, buttonShadowPaint)
                    canvas.drawRoundRect(barrierButtonRect, 12f, 12f, buttonBgPaint)
                    // Use barrier color for border
                    val barrierBorderPaint = Paint().apply {
                        color = barrierColor
                        style = Paint.Style.STROKE
                        strokeWidth = 5f
                        isAntiAlias = true
                    }
                    canvas.drawRoundRect(barrierButtonRect, 12f, 12f, barrierBorderPaint)
                    val barrierTextPaint = Paint().apply {
                        color = barrierColor
                        textSize = 42f
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        isAntiAlias = true
                    }
                    canvas.drawText(barrierLabel, barrierButtonRect.centerX(), barrierButtonRect.centerY() - 20f, barrierTextPaint)
                    canvas.drawText("Level $clickerBarrierLevel", barrierButtonRect.centerX(), barrierButtonRect.centerY() + 10f, buttonDetailPaint)
                    canvas.drawText("Cost: 10 orbs", barrierButtonRect.centerX(), barrierButtonRect.centerY() + 35f, buttonDetailPaint)
                    buttonY -= buttonHeight + buttonGap

                    // Button 3: Speed
                    speedButtonRect = RectF(buttonStartX, buttonY, buttonStartX + buttonWidth, buttonY + buttonHeight)
                    drawButton(speedButtonRect, "SPEED+", clickerSpeedLevel, 10)
                    buttonY -= buttonHeight + buttonGap

                    // Button 2: Fire Rate
                    fireButtonRect = RectF(buttonStartX, buttonY, buttonStartX + buttonWidth, buttonY + buttonHeight)
                    drawButton(fireButtonRect, "FIRE RATE+", clickerFireRateLevel, 10)
                    buttonY -= buttonHeight + buttonGap

                    // Button 1: Damage (at top)
                    damageButtonRect = RectF(buttonStartX, buttonY, buttonStartX + buttonWidth, buttonY + buttonHeight)
                    drawButton(damageButtonRect, "DAMAGE+", clickerDamageLevel, 10)
                }
            }
        }

        // Draw menu tabs on top of everything (always visible)
        val tabPaint = Paint().apply {
            color = Color.argb(255, 0, 180, 180) // Solid bright cyan
            isAntiAlias = true
        }
        val tabActivePaint = Paint().apply {
            color = Color.argb(255, 0, 255, 100) // Bright green for active tab
            isAntiAlias = true
        }
        val tabShadowPaint = Paint().apply {
            color = Color.argb(200, 0, 0, 0)
            isAntiAlias = true
        }
        val tabTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 56f  // Increased from 32f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(8f, 3f, 3f, Color.BLACK)
        }
        val tabBorderPaint = Paint().apply {
            color = Color.CYAN
            style = Paint.Style.STROKE
            strokeWidth = 6f  // Increased from 4f
            isAntiAlias = true
        }

        // Settings tab (top) - square button
        val settingsShadowRect = RectF(
            settingsTabRect.left + 4f,
            settingsTabRect.top + 4f,
            settingsTabRect.right + 4f,
            settingsTabRect.bottom + 4f
        )
        canvas.drawRect(settingsShadowRect, tabShadowPaint)
        canvas.drawRect(
            settingsTabRect,
            if (currentMenu == "SETTINGS") tabActivePaint else tabPaint
        )
        canvas.drawRect(settingsTabRect, tabBorderPaint)

        // Settings gear icon (2x bigger)
        val gearIconPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f  // Doubled from 3f
            isAntiAlias = true
        }
        val gearCenterX = settingsTabRect.centerX()
        val gearCenterY = settingsTabRect.centerY()
        canvas.drawCircle(gearCenterX, gearCenterY, 30f, gearIconPaint)  // Doubled from 15f
        canvas.drawCircle(gearCenterX, gearCenterY, 12f, gearIconPaint)  // Doubled from 6f
        // Gear teeth (4 lines extending from center) - doubled sizes
        canvas.drawLine(gearCenterX - 36f, gearCenterY, gearCenterX - 24f, gearCenterY, gearIconPaint)
        canvas.drawLine(gearCenterX + 24f, gearCenterY, gearCenterX + 36f, gearCenterY, gearIconPaint)
        canvas.drawLine(gearCenterX, gearCenterY - 36f, gearCenterX, gearCenterY - 24f, gearIconPaint)
        canvas.drawLine(gearCenterX, gearCenterY + 24f, gearCenterX, gearCenterY + 36f, gearIconPaint)

        // Upgrades tab (bottom) - square button
        val upgradesShadowRect = RectF(
            upgradesTabRect.left + 4f,
            upgradesTabRect.top + 4f,
            upgradesTabRect.right + 4f,
            upgradesTabRect.bottom + 4f
        )
        canvas.drawRect(upgradesShadowRect, tabShadowPaint)
        canvas.drawRect(
            upgradesTabRect,
            if (currentMenu == "UPGRADES") tabActivePaint else tabPaint
        )
        canvas.drawRect(upgradesTabRect, tabBorderPaint)

        // Dollar sign with orb count (2x bigger)
        val dollarPaint = Paint().apply {
            color = Color.GREEN
            textSize = 72f  // Doubled from 36f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val orbCountPaint = Paint().apply {
            color = Color.WHITE
            textSize = 32f  // Increased from 18f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("$", upgradesTabRect.centerX(), upgradesTabRect.centerY() - 10f, dollarPaint)
        canvas.drawText(orbCurrency.toString(), upgradesTabRect.centerX(), upgradesTabRect.centerY() + 45f, orbCountPaint)

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

        // Draw crash error overlay (if crashed)
        if (crashError != null) {
            // Semi-transparent red background
            val crashBgPaint = Paint().apply {
                color = Color.argb(240, 40, 0, 0)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, w, h, crashBgPaint)

            // Error title
            val titlePaint = Paint().apply {
                color = Color.RED
                textSize = 50f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("CRASH DETECTED", w / 2f, 100f, titlePaint)

            // Error message
            val msgPaint = Paint().apply {
                color = Color.WHITE
                textSize = 24f
                textAlign = Paint.Align.LEFT
                typeface = Typeface.MONOSPACE
                isAntiAlias = true
            }

            var yPos = 200f
            val lines = crashErrorText.split("\n")
            for (line in lines.take(30)) { // Show first 30 lines
                canvas.drawText(line, 40f, yPos, msgPaint)
                yPos += 30f
            }

            // Screenshot hint
            val hintPaint = Paint().apply {
                color = Color.YELLOW
                textSize = 28f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("TAKE SCREENSHOT NOW!", w / 2f, h - 100f, hintPaint)
            canvas.drawText("Log: ${CrashLogger.getLogPath()}", w / 2f, h - 60f, hintPaint)
        }
    }

    fun showCrashError(error: Throwable) {
        crashError = error
        crashErrorText = buildString {
            append("${error.javaClass.simpleName}: ${error.message}\n\n")
            append("Stack trace:\n")
            error.stackTrace.take(20).forEach { element ->
                append("  at $element\n")
            }
        }
        invalidate()
    }
}
