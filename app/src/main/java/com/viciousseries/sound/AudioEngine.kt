package com.viciousseries.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class SoundState { HEALTHY, WARNING, CRITICAL, CRASHED }

private const val SAMPLE_RATE = 44100

class AudioEngine(private val baseFreq: Float = 220f) {

    @Volatile private var state: SoundState = SoundState.HEALTHY
    @Volatile private var pulseHz: Float = 1f
    @Volatile private var running = false
    private var thread: Thread? = null

    private var phase = 0.0

    fun setState(newState: SoundState, newPulseHz: Float? = null) {
        state = newState
        if (newPulseHz != null) pulseHz = newPulseHz
    }

    fun start() {
        if (running) return
        running = true
        thread = Thread { runLoop() }.apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running = false
        thread?.join(500)
        thread = null
    }

    private fun bufferDuration(): Double {
        val d = if (pulseHz > 0f) 1.0 / pulseHz else 1.0
        return d.coerceIn(0.05, 1.0)
    }

    private fun runLoop() {
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            minBuf.coerceAtLeast(SAMPLE_RATE / 10),
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        track.play()

        try {
            while (running) {
                val duration = bufferDuration()
                val samples = renderBuffer(state, duration)
                track.write(samples, 0, samples.size)
            }
        } finally {
            track.stop()
            track.release()
        }
    }

    private fun renderBuffer(currentState: SoundState, duration: Double): ShortArray {
        val n = (SAMPLE_RATE * duration).toInt().coerceAtLeast(1)
        val out = ShortArray(n)

        when (currentState) {
            SoundState.HEALTHY -> {
                val freq = baseFreq
                for (i in 0 until n) {
                    phase += 2 * PI * freq / SAMPLE_RATE
                    out[i] = (sin(phase) * 0.25 * Short.MAX_VALUE).toInt().toShort()
                }
            }
            SoundState.WARNING -> {
                val freq = baseFreq * 1.5f
                for (i in 0 until n) {
                    phase += 2 * PI * freq / SAMPLE_RATE
                    out[i] = (sin(phase) * 0.35 * Short.MAX_VALUE).toInt().toShort()
                }
            }
            SoundState.CRITICAL -> {
                val freq = baseFreq * 2f
                val period = SAMPLE_RATE / freq
                for (i in 0 until n) {
                    val square = if ((i % period.toInt().coerceAtLeast(1)) < period / 2) 1.0 else -1.0
                    val noise = Random.nextDouble(-1.0, 1.0)
                    val blended = square * 0.7 + noise * 0.3
                    out[i] = (blended * 0.5 * Short.MAX_VALUE).toInt().toShort()
                }
            }
            SoundState.CRASHED -> {
                val startFreq = baseFreq * 2
                val endFreq = baseFreq * 0.25f
                for (i in 0 until n) {
                    val t = i.toDouble() / n
                    val freq = startFreq + (endFreq - startFreq) * t
                    phase += 2 * PI * freq / SAMPLE_RATE
                    out[i] = (sin(phase) * 0.5 * Short.MAX_VALUE).toInt().toShort()
                }
            }
        }
        return out
    }
}
