package com.example.neonsurvivor

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import kotlin.math.*

object AudioManager {

    private var musicTrack: AudioTrack? = null
    private var musicThread: Thread? = null
    private var isPlaying = false

    private var rainTrack: AudioTrack? = null
    private var rainThread: Thread? = null
    private var rainPlaying = false

    private val sampleRate = 44100

    fun startMusic(context: Context) {
        if (isPlaying) return

        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("music_enabled", true)) return

        isPlaying = true

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

                // Darksynth bassline: C2, D#2, F2, G2
                val notes = doubleArrayOf(65.41, 77.78, 87.31, 98.00)
                val beatLength = sampleRate / 2 // 0.5 seconds per note

                var noteIndex = 0
                var beatCount = 0

                while (isPlaying) {
                    val note = notes[noteIndex]
                    val buffer = ShortArray(beatLength)

                    for (i in buffer.indices) {
                        val t = i.toDouble() / sampleRate
                        // Dark synth: saw wave + sub bass
                        val saw = (2.0 * ((t * note) % 1.0) - 1.0)
                        val sub = sin(2.0 * PI * note * t * 0.5) // Sub bass
                        val envelope = exp(-t * 2.0) * 0.3 + 0.2 // Decay envelope

                        val mixed = (saw * 0.3 + sub * 0.7) * envelope
                        buffer[i] = (mixed * 8000).toInt().coerceIn(-32768, 32767).toShort()
                    }

                    musicTrack?.write(buffer, 0, buffer.size)

                    beatCount++
                    if (beatCount >= 4) {
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

                // White noise filtered for rain sound
                while (rainPlaying) {
                    val buffer = ShortArray(sampleRate / 10) // 0.1 second chunks

                    for (i in buffer.indices) {
                        // Brown noise (filtered white noise) sounds like rain
                        val white = (Math.random() * 2.0 - 1.0)
                        val filtered = white * 0.15 // Quiet background rain
                        buffer[i] = (filtered * 3000).toInt().coerceIn(-32768, 32767).toShort()
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
