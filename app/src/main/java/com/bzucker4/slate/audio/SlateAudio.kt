package com.bzucker4.slate.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.bzucker4.slate.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SlateAudio(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val scrubSoundId: Int
    private val chimeSoundId: Int
    private val humSoundId: Int
    private var scrubStreamId: Int = 0
    private var humStreamId: Int = 0
    private var idleStopJob: Job? = null
    private var released = false
    private var scrubLoaded = false
    private var chimeLoaded = false
    private var humLoaded = false
    private var humWanted = false
    private var humMuted = false

    init {
        scrubSoundId = pool.load(context, R.raw.scrub, 1)
        chimeSoundId = pool.load(context, R.raw.completion_chime, 1)
        humSoundId = pool.load(context, R.raw.hum, 1)
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status != 0) return@setOnLoadCompleteListener
            when (sampleId) {
                scrubSoundId -> scrubLoaded = true
                chimeSoundId -> chimeLoaded = true
                humSoundId -> {
                    humLoaded = true
                    if (humWanted) playHum()
                }
            }
        }
    }

    fun startOrUpdateScrub(speedPxPerMs: Float) {
        if (released || !scrubLoaded || speedPxPerMs < MOVING_SPEED) {
            if (speedPxPerMs < MOVING_SPEED) stopScrub()
            return
        }
        val rate = pitchRate(speedPxPerMs)
        if (scrubStreamId == 0) {
            scrubStreamId = pool.play(scrubSoundId, 1f, 1f, 1, -1, rate)
            if (scrubStreamId == 0) return
        } else {
            pool.setRate(scrubStreamId, rate)
        }
        idleStopJob?.cancel()
        idleStopJob = scope.launch {
            delay(IDLE_STOP_MS)
            stopScrub()
        }
    }

    fun stopScrub() {
        idleStopJob?.cancel()
        idleStopJob = null
        if (scrubStreamId != 0) {
            pool.stop(scrubStreamId)
            scrubStreamId = 0
        }
    }

    fun startHum() {
        if (released) return
        humWanted = true
        playHum()
    }

    fun stopHum() {
        humWanted = false
        if (humStreamId != 0) {
            pool.stop(humStreamId)
            humStreamId = 0
        }
    }

    fun setHumMuted(muted: Boolean) {
        humMuted = muted
        if (humStreamId != 0) {
            val volume = if (muted) 0f else HUM_VOLUME
            pool.setVolume(humStreamId, volume, volume)
        }
    }

    private fun playHum() {
        if (released || !humLoaded || !humWanted || humStreamId != 0) return
        val volume = if (humMuted) 0f else HUM_VOLUME
        humStreamId = pool.play(humSoundId, volume, volume, 1, -1, 1f)
    }

    fun playCompletionChime() {
        if (released || !chimeLoaded) return
        stopScrub()
        pool.play(chimeSoundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        released = true
        stopScrub()
        stopHum()
        scope.cancel()
        pool.release()
    }

    companion object {
        private const val MOVING_SPEED = 0.04f
        private const val IDLE_STOP_MS = 80L
        private const val MIN_RATE = 0.55f
        private const val MAX_RATE = 1.55f
        private const val HUM_VOLUME = 0.06f

        fun pitchRate(
            speedPxPerMs: Float,
            slowSpeedPxPerMs: Float = 0.12f,
            fastSpeedPxPerMs: Float = 2.8f,
        ): Float {
            val span = (fastSpeedPxPerMs - slowSpeedPxPerMs).coerceAtLeast(0.001f)
            val t = ((speedPxPerMs - slowSpeedPxPerMs) / span).coerceIn(0f, 1f)
            return MIN_RATE + (MAX_RATE - MIN_RATE) * t
        }
    }
}
