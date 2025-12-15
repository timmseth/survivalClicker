# Neon Survivor

A cyberpunk-themed survival roguelike for Android, inspired by Vampire Survivors, Holocure, and Brotato. Fight waves of enemies, collect upgrades, and survive in a neon-drenched dystopian world.

## Features

### Core Gameplay
- **Virtual Joystick Control** - Smooth joystick works anywhere on screen for precise movement
- **Auto-Fire Combat** - Automatic bullet firing system with intelligent targeting
- **Wave-Based Progression** - Survive increasingly difficult enemy waves
- **Boss Waves** - Face powerful bosses every 5 waves (Ball and Chain Bot with 360° attack patterns)
- **Dual Upgrade System**
  - **Clicker Upgrades** - Permanent stat boosts (damage, fire rate, HP, barrier, drones)
  - **Gacha Power-Ups** - Random temporary power-ups dropped by enemies

### Enemy Types
- **Zombies** - Melee enemies that chase the player (animated with 3 character variants)
- **Archers** - Ranged enemies that shoot single projectiles (waves 4+)
- **Shotgunners** - Dangerous spread-shot enemies (waves 7+)
- **Ball and Chain Bot** - Boss enemy with 360° rotating attack pattern (every 5 waves)
- **Elite Variants** - Tougher enemies with guaranteed power-up drops
- **Breather Waves** - Recovery waves with no enemies (every 10 waves)

### Power-Up System (20+ Types)
Enemies drop power-ups that dramatically enhance your abilities:

**Offensive**
- **Spread Shot** - Fire 5 bullets in an arc
- **Multishot** - Fire 3 bullets simultaneously
- **Rapid Fire** - Massively increased fire rate
- **Piercing Shots** - Bullets pass through enemies
- **Homing Bullets** - Bullets track enemies
- **Giant Bullets** - Larger, more powerful projectiles
- **Explosive Rounds** - Bullets create blast radius on impact
- **Bouncy Shots** - Bullets ricochet off walls
- **Laser Sight** - Precise targeting with increased accuracy

**Defensive**
- **Speed Boost** - Increased movement speed
- **Shield** - Temporary invulnerability
- **Vampire** - Lifesteal on enemy kills
- **Health** - Instant HP restoration

**Utility**
- **Magnet** - Attract green orbs from distance
- **Bullet Time** - Slow down time for tactical advantage
- **Orbital** - Summon rotating defensive orbs
- **Shockwave** - Damage nearby enemies on hit

### Clicker Upgrade Tree
Spend green orbs to permanently upgrade:
- **Damage** - Increase bullet damage
- **Fire Rate** - Shoot faster
- **Max HP** - Increase health pool
- **Movement Speed** - Move faster
- **Prismatic Barrier** - Protective energy shield with AOE damage at higher levels
- **Quantum Mirror** - Orbital drones that auto-fire at enemies
- **Frost Aura** - Slow nearby enemies

### Visual Features
- **Cyberpunk Neon Aesthetic** - Dark backgrounds with vibrant cyan/magenta accents
- **Epic Splash Screen** - Flickering neon title with 3-layer parallax rain animation
- **Animated Sprites**
  - Player: idle, run, hit, death animations
  - Zombies: directional movement with 3 character variants
  - Archers: idle and run states
  - Shotgunners: idle and run states
  - Boss: animated sprite sheet
- **Particle Effects**
  - Cyan blood splatter on enemy death
  - Bullet glows and trails
  - Power-up pickup effects
- **Screen Effects**
  - Screen shake on damage
  - Red damage flash overlay
  - Wave complete announcements
  - Power-up pickup feedback
- **Retro Grid Background** - Animated scrolling grid for movement feel
- **Debug Visualization** - Subtle pink circles show enemy hitboxes (15% opacity)

### Game Balance
- **Player Stats**
  - Base damage: 8
  - Fire rate: 1.5 shots/sec
  - Movement speed: 250
  - Starting HP: 100
  - God mode: Available for testing (hold pause button)

- **Enemy Scaling**
  - Count: 8 + wave × 3
  - Speed: 100 + wave × 15
  - HP: 30 + wave × 10
  - Zombie radius: 30f (25% larger than other enemies)
  - Standard radius: 24f

- **Wave Progression**
  - Waves 1-3: Zombies only
  - Waves 4-6: Zombies + Archers (70% zombie, 30% archer)
  - Wave 7+: Full mix (50% zombie, 30% archer, 20% shotgunner)
  - Boss waves: Every 5 waves (5, 10, 15, etc.)
  - Breather waves: Every 10 waves (no enemies, recovery time)

### Quality of Life
- **Pause Menu** - Access stats and resume/restart
- **Enemy Proximity Warning** - Visual indicator when enemies are near
- **Power-Up Drop System** - Varied drop rates (zombies 10%, archers 20%, shotgunners 30%, bosses 100%)
- **Green Orb Currency** - Collect orbs to unlock permanent upgrades
- **Wall Obstacles** - Pink barriers create tactical challenges (scales with wave)
- **Damage Cooldown** - 0.5s invulnerability after hit prevents instant death
- **Haptic Feedback** - Vibration on damage

### Test Mode
- **Wave 1 Test** - Spawns one of each enemy type for testing
- Enabled for development and debugging

## Build & Deploy

### Automatic Builds
Every push to `main` triggers GitHub Actions CI:
1. Builds APK with Gradle
2. Uploads artifact to GitHub Actions
3. Emails APK to configured recipients

### Manual Testing
1. Download APK from GitHub Actions artifacts
2. Install on Android device (enable "Install from unknown sources")
3. Launch and test

### Email Setup
See [EMAIL_SETUP.md](EMAIL_SETUP.md) for configuring automated APK delivery via email.

## Tech Stack
- **Language**: Kotlin
- **Platform**: Android (native)
- **Graphics**: Canvas 2D rendering with custom sprite animation
- **Min SDK**: Android 5.0+ (API 21)
- **CI/CD**: GitHub Actions with automated APK delivery
- **Build Tool**: Gradle 8.7

## Project Structure
```
NeonSurvivor/
├── app/src/main/
│   ├── java/com/example/neonsurvivor/
│   │   ├── SplashActivity.kt    # Neon splash screen with parallax rain
│   │   ├── MainActivity.kt       # Game activity wrapper
│   │   ├── GameView.kt           # Main game loop, rendering, and logic
│   │   └── CrashLogger.kt        # Debug logging system
│   ├── res/drawable/
│   │   ├── player_idle.png       # Player sprite sheet (4 frames)
│   │   ├── player_run.png        # Player run animation (6 frames)
│   │   ├── player_hit.png        # Player hit animation (2 frames)
│   │   ├── player_death.png      # Player death animation (5 frames)
│   │   ├── enemies.png           # Zombie sprite sheet (27 sprites, 3 characters)
│   │   ├── archer_idle_*.png     # Archer idle frames (5 frames)
│   │   ├── archer_run_*.png      # Archer run frames (8 frames)
│   │   ├── shotgunner_idle_*.png # Shotgunner idle frames (6 frames)
│   │   └── shotgunner_run_*.png  # Shotgunner run frames (8 frames)
│   └── AndroidManifest.xml
├── .github/workflows/
│   └── android.yml               # CI/CD pipeline
└── build.gradle
```

## Development Process

This project is developed incrementally using an AI-assisted workflow:
1. Propose feature/improvement
2. Implement changes
3. Commit to git
4. CI builds and deploys APK
5. Manual testing on device
6. Feedback and iteration

Each cycle focuses on stability and incremental improvements.

## Recent Updates

### Sprite System Overhaul
- Implemented frame-based animation for all enemy types
- Movement detection system (idle vs running states)
- Aspect ratio preservation in sprite rendering
- Bottom-center anchor point for proper grounding
- Directional sprite flipping
- Per-enemy-type positioning and scaling

### Enemy System Expansion
- Added Archer and Shotgunner enemy types
- Boss enemy with unique attack patterns
- Elite enemy variants with guaranteed drops
- Progressive enemy introduction by wave

### Upgrade System
- Dual menu system (Clicker vs Gacha)
- 7 permanent upgrade trees
- 20+ temporary power-ups with stacking effects
- Green orb currency economy

### Visual Polish
- Animated sprites for player and all enemy types
- Particle effects and screen shake
- Wave completion feedback
- Power-up pickup animations
- Debug visualization for hitboxes

## Planned Features
- 🎵 **Custom Music System** - Generative cyberpunk soundtrack
- 🎨 **More Visual Effects** - Glitch effects, screen distortion
- 🔫 **Weapon Evolution** - Fusion system for power-ups
- ⚙️ **Settings Screen** - Volume, vibration, difficulty controls
- 📊 **Meta Progression** - Persistent unlocks, character selection, achievements

## Known Issues
- Test mode currently enabled for wave 1 (will be disabled for release)
- Boss sprite sheet needs animation implementation (currently static)
- Some enemy positioning may need fine-tuning

## Credits
Developed with AI assistance using Claude Code extension for VSCode.

Inspired by:
- Vampire Survivors (poncle)
- Holocure (Kay Yu)
- Brotato (Blobfish)
