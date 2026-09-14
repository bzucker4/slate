package com.bzucker4.slate.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import kotlinx.coroutines.launch

@Composable
fun ScratchScreen(
    onCleared: () -> Unit,
    onScrubMove: (speedPxPerMs: Float, distancePx: Float) -> Unit,
    onScrubStop: () -> Unit,
    onDissolveStart: () -> Unit,
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
    val dissolving = remember(layer) { mutableStateOf(false) }
    val dissolve = remember(layer) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val onClearedState = rememberUpdatedState(onCleared)
    val onDissolveStartState = rememberUpdatedState(onDissolveStart)
    val onScrubStopState = rememberUpdatedState(onScrubStop)
    val frostPaint = remember { android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG) }

    fun beginDissolve() {
        if (dissolving.value) return
        dissolving.value = true
        onScrubStopState.value()
        onDissolveStartState.value()
        scope.launch {
            dissolve.snapTo(0f)
            dissolve.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing),
            )
            onClearedState.value()
        }
    }

    DisposableEffect(layer) {
        onDispose { layer?.recycle() }
    }
    DisposableEffect(onScrubStop) {
        onDispose { onScrubStop() }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { canvasSize = it }
            .pointerInput(layer, minRadiusPx, maxRadiusPx, onScrubMove, onScrubStop) {
                val scratch = layer ?: return@pointerInput
                awaitEachGesture {
                    if (dissolving.value) return@awaitEachGesture
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var last = down.position
                    var lastTime = down.uptimeMillis
                    var lastRadius = maxRadiusPx
                    scratch.stampCircle(last.x, last.y, lastRadius)
                    if (scratch.clearedRatio >= Scratch.CLEAR_THRESHOLD) {
                        beginDissolve()
                    }
                    revision++
                    down.consume()
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.first()
                            if (!change.pressed) {
                                onScrubStop()
                                if (scratch.clearedRatio >= Scratch.CLEAR_THRESHOLD) {
                                    beginDissolve()
                                }
                                break
                            }
                            if (dissolving.value) {
                                onScrubStop()
                                change.consume()
                                continue
                            }
                            val dt = (change.uptimeMillis - lastTime).coerceAtLeast(1L).toFloat()
                            val distance = Scratch.distance(last.x, last.y, change.position.x, change.position.y)
                            val speed = distance / dt
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
                            onScrubMove(speed, distance)
                            last = change.position
                            lastTime = change.uptimeMillis
                            lastRadius = radius
                            revision++
                            if (scratch.clearedRatio >= Scratch.CLEAR_THRESHOLD) {
                                beginDissolve()
                            }
                            change.consume()
                        }
                    } finally {
                        onScrubStop()
                    }
                }
            },
    ) {
        revision
        dissolve.value
        val scratch = layer ?: return@Canvas
        if (scratch.bitmap.isRecycled) return@Canvas
        frostPaint.alpha = ((1f - dissolve.value).coerceIn(0f, 1f) * 255f).toInt()
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawBitmap(scratch.bitmap, 0f, 0f, frostPaint)
        }
    }
}

@Preview
@Composable
private fun ScratchScreenPreview() {
    ScratchScreen(
        onCleared = {},
        onScrubMove = { _, _ -> },
        onScrubStop = {},
        onDissolveStart = {},
    )
}
