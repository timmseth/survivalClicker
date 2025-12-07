# Neon Survivor

A cyberpunk-themed survival roguelike for Android, inspired by Vampire Survivors, Holocure, and Brotato. Fight waves of enemies, collect upgrades, and survive in a neon-drenched dystopian world.

## Features

### Core Gameplay
- **Virtual Joystick Control** - Move anywhere on screen with smooth joystick input
- **Auto-Fire Combat** - Automatic bullet firing system, focus on movement and positioning
- **Wave-Based Progression** - Survive increasingly difficult enemy waves
- **Gacha Upgrade System** - Choose from random upgrades between waves
  - Damage boost (+20%)
  - Fire rate increase (+20%)
  - Movement speed (+15%)
  - HP boost (+20 max HP + full heal)

### Visual Polish
- **Cyberpunk Neon Aesthetic** - Dark backgrounds with vibrant cyan/magenta accents
- **Epic Splash Screen** - Flickering neon title with 3-layer parallax rain animation
- **Damage Feedback System**
  - Screen shake on hit
  - Red damage flash overlay
  - Haptic vibration feedback
- **Blood Particle Effects** - Cyan blood splatter when enemies die
- **Retro Grid Background** - Animated scrolling grid for movement feel

### Game Balance (Difficulty Rebalanced - Cycle 2)
- **Player Stats**
  - Base damage: 8
  - Fire rate: 1.5 shots/sec
  - Movement speed: 250
  - Starting HP: 100
- **Enemy Scaling**
  - Count: 8 + wave × 3
  - Speed: 100 + wave × 15
  - HP: 30 + wave × 10
- **Damage System**
  - 25 damage per hit with 0.5s cooldown (prevents instant death)

## Development Roadmap

### Completed (Cycles 1-3)
- ✅ Damage feedback system (screen shake, flash, haptics)
- ✅ Difficulty rebalancing (player weaker, enemies tankier)
- ✅ Cyberpunk splash screen with neon rain
- ✅ Return to splash on death
- ✅ CI/CD with GitHub Actions (auto-build APKs)
- ✅ Email delivery of APK builds

### Planned Features
- 🎵 **Custom Music System** - Strudel/Tidal Cycles integration for advanced generative cyberpunk soundtrack
- 🎮 **Joystick Enhancement** - Make joystick work anywhere on screen (currently left-bottom half)
- 🎨 **Visual Upgrades**
  - Darker in-game background (more cyberpunk atmosphere)
  - Neon glow effects on player/bullets
  - Screen distortion/glitch effects
- 👾 **Enemy Variety**
  - Multiple enemy types (fast, tank, ranged)
  - Boss waves every 5-10 waves
  - Special attack patterns
- 🔫 **Weapon System**
  - Multiple weapon types (Vampire Survivors style)
  - Weapon evolution/fusion system
  - Passive items and synergies
- ⚙️ **Settings Screen**
  - Volume controls
  - Vibration toggle
  - Difficulty selection
- 📊 **Meta Progression**
  - Persistent unlocks between runs
  - Character selection
  - Achievement system

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
- **Graphics**: Canvas 2D rendering
- **Min SDK**: Android 5.0+ (API 21)
- **CI/CD**: GitHub Actions
- **Build Tool**: Gradle 8.7

## Project Structure
```
NeonSurvivor/
├── app/src/main/java/com/example/neonsurvivor/
│   ├── SplashActivity.kt    # Neon splash screen with parallax rain
│   ├── MainActivity.kt       # Game activity wrapper
│   └── GameView.kt           # Main game loop and rendering
├── app/src/main/AndroidManifest.xml
└── app/build.gradle
```

## Development Process

This project is developed incrementally using an AI-assisted workflow:
1. Propose feature/improvement
2. Implement changes
3. Commit to git
4. CI builds APK
5. Manual testing
6. Feedback → Next cycle

Each cycle focuses on one feature or improvement to maintain stability.
