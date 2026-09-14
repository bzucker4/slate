package com.bzucker4.slate.scratch

import kotlin.math.sqrt

object Scratch {
    const val CLEAR_THRESHOLD = 0.85f

    fun brushRadiusPx(
        speedPxPerMs: Float,
        minRadiusPx: Float,
        maxRadiusPx: Float,
        slowSpeedPxPerMs: Float = 0.12f,
        fastSpeedPxPerMs: Float = 2.8f,
    ): Float {
        val span = (fastSpeedPxPerMs - slowSpeedPxPerMs).coerceAtLeast(0.001f)
        val t = ((speedPxPerMs - slowSpeedPxPerMs) / span).coerceIn(0f, 1f)
        return maxRadiusPx + (minRadiusPx - maxRadiusPx) * t
    }

    fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
}
