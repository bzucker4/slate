package com.bzucker4.slate.haptics

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.roundToInt

enum class HapticTier {
    Rich,
    Basic,
    None,
}

class SlateHaptics(context: Context) {
    private val vibrator: Vibrator? = resolveVibrator(context)
    val tier: HapticTier = detectTier(vibrator)

    private var distanceCarry = 0f
    private var lastGritAtMs = 0L

    fun grit(speedPxPerMs: Float, distancePx: Float) {
        if (tier == HapticTier.None || distancePx <= 0f) return
        val speedT = ((speedPxPerMs - 0.12f) / 2.68f).coerceIn(0f, 1f)
        val spacing = 26f - 12f * speedT
        distanceCarry += distancePx
        val now = SystemClock.uptimeMillis()
        val minGapMs = if (tier == HapticTier.Rich) 18L else 36L
        while (distanceCarry >= spacing) {
            distanceCarry -= spacing
            if (now - lastGritAtMs < minGapMs) continue
            lastGritAtMs = now
            pulseGrit(speedT)
        }
    }

    fun stopGrit() {
        distanceCarry = 0f
        vibrator?.cancel()
    }

    fun thunk() {
        when (tier) {
            HapticTier.None -> Unit
            HapticTier.Rich -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    vibrator?.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_THUD) == true
                ) {
                    vibrate(
                        VibrationEffect.startComposition()
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.85f)
                            .compose(),
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                } else {
                    vibrate(VibrationEffect.createOneShot(48L, 220))
                }
            }
            HapticTier.Basic -> vibrate(
                VibrationEffect.createOneShot(40L, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        }
    }

    fun completion() {
        when (tier) {
            HapticTier.None -> Unit
            HapticTier.Rich -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    vibrator?.areAllPrimitivesSupported(
                        VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                        VibrationEffect.Composition.PRIMITIVE_CLICK,
                    ) == true
                ) {
                    vibrate(
                        VibrationEffect.startComposition()
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.7f)
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1f)
                            .compose(),
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                } else {
                    vibrate(VibrationEffect.createWaveform(longArrayOf(0L, 28L, 40L, 55L), -1))
                }
            }
            HapticTier.Basic -> vibrate(
                VibrationEffect.createWaveform(longArrayOf(0L, 30L, 45L, 50L), -1),
            )
        }
    }

    fun release() {
        stopGrit()
    }

    private fun pulseGrit(speedT: Float) {
        when (tier) {
            HapticTier.None -> Unit
            HapticTier.Rich -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    vibrator?.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK) == true
                ) {
                    val scale = 0.28f + 0.45f * speedT
                    vibrate(
                        VibrationEffect.startComposition()
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, scale)
                            .compose(),
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else {
                    val amp = (70 + 90 * speedT).roundToInt().coerceIn(1, 255)
                    vibrate(VibrationEffect.createOneShot(12L, amp))
                }
            }
            HapticTier.Basic -> vibrate(
                VibrationEffect.createOneShot(10L, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        }
    }

    private fun vibrate(effect: VibrationEffect) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        v.vibrate(effect)
    }

    companion object {
        fun detectTier(vibrator: Vibrator?): HapticTier {
            if (vibrator == null || !vibrator.hasVibrator()) return HapticTier.None
            val richPrimitives = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                vibrator.areAllPrimitivesSupported(
                    VibrationEffect.Composition.PRIMITIVE_TICK,
                    VibrationEffect.Composition.PRIMITIVE_CLICK,
                    VibrationEffect.Composition.PRIMITIVE_THUD,
                )
            return if (richPrimitives || vibrator.hasAmplitudeControl()) {
                HapticTier.Rich
            } else {
                HapticTier.Basic
            }
        }

        private fun resolveVibrator(context: Context): Vibrator? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                context.getSystemService(Vibrator::class.java)
            }
        }
    }
}

/** Haptic tiers and scratch / dissolve feedback (`SlateHaptics`). */
object Haptics {
    fun detectTier(vibrator: Vibrator?): HapticTier = SlateHaptics.detectTier(vibrator)
}
