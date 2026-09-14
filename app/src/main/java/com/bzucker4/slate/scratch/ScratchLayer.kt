package com.bzucker4.slate.scratch

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class ScratchLayer(
    val width: Int,
    val height: Int,
    private val cellSize: Int = 8,
) {
    val bitmap: Bitmap = FrostTexture.create(width, height)
    var clearedRatio: Float = 0f
        private set

    private val canvas = Canvas(bitmap)
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.FILL
    }
    private val cols = max(1, ceil(width / cellSize.toFloat()).toInt())
    private val rows = max(1, ceil(height / cellSize.toFloat()).toInt())
    private val cellCount = cols * rows
    private val cleared = BooleanArray(cellCount)
    private var clearedCells = 0

    fun stampCircle(cx: Float, cy: Float, radius: Float) {
        if (radius <= 0f) return
        canvas.drawCircle(cx, cy, radius, clearPaint)
        markCoverage(cx, cy, radius)
    }

    fun stampSegment(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        radiusStart: Float,
        radiusEnd: Float,
    ) {
        val distance = Scratch.distance(x1, y1, x2, y2)
        val spacing = (minOf(radiusStart, radiusEnd) * 0.45f).coerceAtLeast(2f)
        val steps = max(1, ceil(distance / spacing).toInt())
        for (i in 1..steps) {
            val t = i / steps.toFloat()
            val x = x1 + (x2 - x1) * t
            val y = y1 + (y2 - y1) * t
            val r = radiusStart + (radiusEnd - radiusStart) * t
            stampCircle(x, y, r)
        }
    }

    fun recycle() {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }

    private fun markCoverage(cx: Float, cy: Float, radius: Float) {
        val minCol = floor((cx - radius) / cellSize).toInt().coerceIn(0, cols - 1)
        val maxCol = ceil((cx + radius) / cellSize).toInt().coerceIn(0, cols - 1)
        val minRow = floor((cy - radius) / cellSize).toInt().coerceIn(0, rows - 1)
        val maxRow = ceil((cy + radius) / cellSize).toInt().coerceIn(0, rows - 1)
        val radiusSq = radius * radius
        val half = cellSize * 0.5f
        for (row in minRow..maxRow) {
            for (col in minCol..maxCol) {
                val index = row * cols + col
                if (cleared[index]) continue
                val cellX = col * cellSize + half
                val cellY = row * cellSize + half
                val dx = cellX - cx
                val dy = cellY - cy
                if (dx * dx + dy * dy <= radiusSq) {
                    cleared[index] = true
                    clearedCells++
                }
            }
        }
        clearedRatio = clearedCells / cellCount.toFloat()
    }
}
