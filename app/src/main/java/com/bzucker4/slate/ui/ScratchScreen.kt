package com.bzucker4.slate.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.bzucker4.slate.scratch.Scratch
import com.bzucker4.slate.scratch.ScratchLayer

@Composable
fun ScratchScreen(
    onCleared: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val minRadiusPx = with(density) { 14.dp.toPx() }
    val maxRadiusPx = with(density) { 96.dp.toPx() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var revision by remember { mutableIntStateOf(0) }
    val layer = remember(canvasSize.width, canvasSize.height) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) {
            null
        } else {
            ScratchLayer(canvasSize.width, canvasSize.height)
        }
    }
    val finished = remember(layer) { mutableStateOf(false) }

    DisposableEffect(layer) {
        onDispose { layer?.recycle() }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { canvasSize = it }
            .pointerInput(layer, minRadiusPx, maxRadiusPx, onCleared) {
                val scratch = layer ?: return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var last = down.position
                    var lastTime = down.uptimeMillis
                    var lastRadius = maxRadiusPx
                    scratch.stampCircle(last.x, last.y, lastRadius)
                    revision++
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.first()
                        if (!change.pressed) {
                            if (!finished.value && scratch.clearedRatio >= Scratch.CLEAR_THRESHOLD) {
                                finished.value = true
                                onCleared()
                            }
                            break
                        }
                        val dt = (change.uptimeMillis - lastTime).coerceAtLeast(1L).toFloat()
                        val speed = Scratch.distance(last.x, last.y, change.position.x, change.position.y) / dt
                        val radius = Scratch.brushRadiusPx(
                            speedPxPerMs = speed,
                            minRadiusPx = minRadiusPx,
                            maxRadiusPx = maxRadiusPx,
                        )
                        scratch.stampSegment(
                            x1 = last.x,
                            y1 = last.y,
                            x2 = change.position.x,
                            y2 = change.position.y,
                            radiusStart = lastRadius,
                            radiusEnd = radius,
                        )
                        last = change.position
                        lastTime = change.uptimeMillis
                        lastRadius = radius
                        revision++
                        change.consume()
                    }
                }
            },
    ) {
        revision
        val scratch = layer ?: return@Canvas
        if (scratch.bitmap.isRecycled) return@Canvas
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawBitmap(scratch.bitmap, 0f, 0f, null)
        }
    }
}

@Preview
@Composable
private fun ScratchScreenPreview() {
    ScratchScreen(onCleared = {})
}
