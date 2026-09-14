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
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val scrubSoundId: Int
    private val chimeSoundId: Int
    private var scrubStreamId: Int = 0
    private var idleStopJob: Job? = null
    private var released = false
    private var scrubLoaded = false
    private var chimeLoaded = false

    init {
        scrubSoundId = pool.load(context, R.raw.scrub, 1)
        chimeSoundId = pool.load(context, R.raw.completion_chime, 1)
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status != 0) return@setOnLoadCompleteListener
            when (sampleId) {
                scrubSoundId -> scrubLoaded = true
                chimeSoundId -> chimeLoaded = true
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

    /**
     * Short sub-bass hit. Call when frost dissolve finishes.
     * Dissolve is not implemented yet; keep this as the completion hook.
     */
    fun playCompletionChime() {
        if (released || !chimeLoaded) return
        stopScrub()
        pool.play(chimeSoundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        released = true
        stopScrub()
        scope.cancel()
        pool.release()
    }

    companion object {
        private const val MOVING_SPEED = 0.04f
        private const val IDLE_STOP_MS = 80L
        private const val MIN_RATE = 0.55f
        private const val MAX_RATE = 1.55f

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
