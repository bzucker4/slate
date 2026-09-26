package com.bzucker4.slate.scratch

import android.graphics.Bitmap
import kotlin.math.floor
import androidx.core.graphics.createBitmap

internal object FrostTexture {
    fun create(width: Int, height: Int): Bitmap {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        val pixels = IntArray(w * h)
        val invH = 1f / h
        var i = 0
        for (y in 0 until h) {
            val gy = y * invH
            for (x in 0 until w) {
                val n =
                    valueNoise(x * 0.085f, y * 0.085f) * 0.52f +
                        valueNoise(x * 0.031f, y * 0.029f) * 0.33f +
                        valueNoise(x * 0.21f, y * 0.19f) * 0.15f
                val cool = (1f - gy) * 10f
                val v = 158f + n * 78f + cool
                val r = v.toInt().coerceIn(0, 255)
                val g = (v + 3f).toInt().coerceIn(0, 255)
                val b = (v + 14f).toInt().coerceIn(0, 255)
                pixels[i++] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        val bitmap = createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setHasAlpha(true)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }

    private fun valueNoise(x: Float, y: Float): Float {
        val x0 = floor(x.toDouble()).toInt()
        val y0 = floor(y.toDouble()).toInt()
        val fx = x - x0
        val fy = y - y0
        val sx = fx * fx * (3f - 2f * fx)
        val sy = fy * fy * (3f - 2f * fy)
        val n00 = hash(x0, y0)
        val n10 = hash(x0 + 1, y0)
        val n01 = hash(x0, y0 + 1)
        val n11 = hash(x0 + 1, y0 + 1)
        val nx0 = n00 + (n10 - n00) * sx
        val nx1 = n01 + (n11 - n01) * sx
        return nx0 + (nx1 - nx0) * sy
    }

    private fun hash(x: Int, y: Int): Float {
        var n = x * 374_761_393 + y * 668_265_263
        n = (n xor (n ushr 13)) * 1_274_126_177
        return ((n ushr 1) and 0x7FFF_FFFF) / 2_147_483_647f
    }
}
