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

        // Load saved volume (default 50% on first run)
        musicVolume = prefs.getFloat("music_volume", 0.5f)

        isPlaying = true

        // Play OGG music file (future: loop through multiple tracks)
        try {
            mediaPlayer = MediaPlayer.create(context, R.raw.portal_underworld)
            mediaPlayer?.isLooping = true
            mediaPlayer?.setVolume(musicVolume, musicVolume)
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Update volume for currently playing music
    fun updateMusicVolume(volume: Float) {
        musicVolume = volume
        mediaPlayer?.setVolume(volume, volume)
    }

    fun stopMusic() {
        isPlaying = false

        // Stop MediaPlayer
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun startRain(context: Context) {
        if (rainPlaying) return

        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("sound_enabled", true)) return

        // Load saved volume (default 30% for ambient sound)
        rainVolume = prefs.getFloat("rain_volume", 0.3f)

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
