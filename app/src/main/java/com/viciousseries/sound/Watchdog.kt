package com.viciousseries.sound

class Watchdog(baseFreq: Float = 220f) {

    companion object {
        const val TIMEOUT_WARNING_S = 1.0
        const val TIMEOUT_CRITICAL_S = 3.0
        const val LOOP_SPEED_WARNING_MS = 50.0
        const val LOOP_SPEED_CRITICAL_MS = 200.0
    }

    val engine = AudioEngine(baseFreq)

    @Volatile private var lastBeatMs: Long? = null
    @Volatile private var running = false
    private var pollThread: Thread? = null

    fun start() {
        if (running) return
        running = true
        engine.start()
        pollThread = Thread { pollLoop() }.apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running = false
        pollThread?.join(500)
        engine.stop()
    }

    fun beat(loopSpeedMs: Double? = null) {
        lastBeatMs = System.currentTimeMillis()
        if (loopSpeedMs != null) {
            when {
                loopSpeedMs >= LOOP_SPEED_CRITICAL_MS -> engine.setState(SoundState.CRITICAL, 4f)
                loopSpeedMs >= LOOP_SPEED_WARNING_MS -> engine.setState(SoundState.WARNING, 2f)
                else -> engine.setState(SoundState.HEALTHY, 1f)
            }
        }
    }

    fun exception(message: String) {
        engine.setState(SoundState.CRASHED, 1f)
    }

    fun reset() {
        lastBeatMs = System.currentTimeMillis()
        engine.setState(SoundState.HEALTHY, 1f)
    }

    private fun pollLoop() {
        while (running) {
            val last = lastBeatMs
            if (last != null) {
                val gapS = (System.currentTimeMillis() - last) / 1000.0
                when {
                    gapS >= TIMEOUT_CRITICAL_S -> engine.setState(SoundState.CRITICAL, 4f)
                    gapS >= TIMEOUT_WARNING_S -> engine.setState(SoundState.WARNING, 2f)
                }
            }
            Thread.sleep(50)
        }
    }
}
