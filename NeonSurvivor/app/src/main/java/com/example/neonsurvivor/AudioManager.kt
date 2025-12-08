package com.example.neonsurvivor

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Build
import kotlin.math.*

object AudioManager {

    private var musicTrack: AudioTrack? = null
    private var musicThread: Thread? = null
    private var isPlaying = false

    private var mediaPlayer: MediaPlayer? = null // For OGG file playback

    private var rainTrack: AudioTrack? = null
    private var rainThread: Thread? = null
    private var rainPlaying = false

    private val sampleRate = 44100

    var musicVolume = 0.12f // 0.0 to 1.0 (default 12% to match rain at 100%)
    var rainVolume = 1.0f // 0.0 to 1.0 (default 100%)

    fun startMusic(context: Context) {
        if (isPlaying) return

        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("music_enabled", true)) return

        // Load saved volume
        musicVolume = prefs.getFloat("music_volume", 0.12f)

        // Check which music track to use
        val usePortalMusic = prefs.getBoolean("use_portal_music", false)

        isPlaying = true

        if (usePortalMusic) {
            // Use Portal to Underworld OGG file
            try {
                mediaPlayer = MediaPlayer.create(context, R.raw.portal_underworld)
                mediaPlayer?.isLooping = true
                mediaPlayer?.setVolume(musicVolume, musicVolume)
                mediaPlayer?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        musicThread = Thread {
            try {
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                musicTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setSampleRate(sampleRate)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        android.media.AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                    )
                }

                musicTrack?.play()

                // Darksynth progression: C2, D#2, F2, G2, G#2, F2, D#2, C2 (longer loop with variety)
                val notes = doubleArrayOf(65.41, 77.78, 87.31, 98.00, 103.83, 87.31, 77.78, 65.41)
                val beatLength = sampleRate / 3 // 0.33 seconds per note (faster pacing with variety)

                var noteIndex = 0
                var beatCount = 0
                var lowPassState = 0.0 // For filtering

                while (isPlaying) {
                    val note = notes[noteIndex]
                    val buffer = ShortArray(beatLength)

                    for (i in buffer.indices) {
                        val t = i.toDouble() / sampleRate
                        // Dark synth: saw wave + sub bass with variation
                        val saw = (2.0 * ((t * note) % 1.0) - 1.0)
                        val sub = sin(2.0 * PI * note * t * 0.5) // Sub bass

                        // Vary envelope for different notes
                        val envelopeDecay = if (noteIndex % 2 == 0) 2.5 else 3.0
                        val envelope = exp(-t * envelopeDecay) * 0.25 + 0.15

                        // Mix with slight variation per note
                        val sawMix = if (noteIndex < 4) 0.25 else 0.3
                        val mixed = (saw * sawMix + sub * (1.0 - sawMix)) * envelope

                        // Simple low-pass filter to soften harsh edges
                        lowPassState = lowPassState * 0.7 + mixed * 0.3

                        buffer[i] = (lowPassState * 6000 * musicVolume).toInt().coerceIn(-32768, 32767).toShort()
                    }

                    musicTrack?.write(buffer, 0, buffer.size)

                    beatCount++
                    if (beatCount >= 3) {
                        beatCount = 0
                        noteIndex = (noteIndex + 1) % notes.size
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        musicThread?.start()
    }

    fun stopMusic() {
        isPlaying = false

        // Stop MediaPlayer if it was being used
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        // Stop procedural music if it was being used
        musicThread?.join(500)
        musicTrack?.stop()
        musicTrack?.release()
        musicTrack = null
        musicThread = null
    }

    fun startRain(context: Context) {
        if (rainPlaying) return

        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("sound_enabled", true)) return

        // Load saved volume
        rainVolume = prefs.getFloat("rain_volume", 1.0f)

        rainPlaying = true

        rainThread = Thread {
            try {
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                rainTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setSampleRate(sampleRate)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        android.media.AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                    )
                }

                rainTrack?.play()

                // Softer filtered noise for gentle rain sound
                var brownState = 0.0 // For brown noise filtering

                while (rainPlaying) {
                    val buffer = ShortArray(sampleRate / 10) // 0.1 second chunks

                    for (i in buffer.indices) {
                        // Brown noise: heavily filtered white noise for soft rain
                        val white = (Math.random() * 2.0 - 1.0)

                        // Multi-stage filtering to remove harsh frequencies
                        brownState = (brownState + white * 0.02) * 0.98 // Brown noise filter
                        val softFiltered = brownState * 0.08 // Much quieter, softer

                        buffer[i] = (softFiltered * 8000 * rainVolume).toInt().coerceIn(-32768, 32767).toShort()
                    }

                    rainTrack?.write(buffer, 0, buffer.size)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        rainThread?.start()
    }

    fun stopRain() {
        rainPlaying = false
        rainThread?.join(500)
        rainTrack?.stop()
        rainTrack?.release()
        rainTrack = null
        rainThread = null
    }

    fun cleanup() {
        stopMusic()
        stopRain()
    }
}
