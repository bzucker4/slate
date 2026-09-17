package com.bzucker4.slate.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bzucker4.slate.lockout.Lockout
import com.bzucker4.slate.lockout.ProximityMonitor

private val FrostbiteGlow = Color(0xFF7FB3C0)
private val FrostbiteMid = Color(0xFF16232B)
private val FrostbiteBase = Color(0xFF0B0E11)

@Composable
fun BlackoutScreen(
    remainingMs: Long,
    humEnabled: Boolean,
    onBlackoutStarted: (humEnabled: Boolean) -> Unit,
    onBlackoutStopped: () -> Unit,
    onPocketCovered: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LaunchedEffect(humEnabled) {
        onBlackoutStarted(humEnabled)
    }
    DisposableEffect(Unit) {
        val monitor = ProximityMonitor(context, onPocketCovered)
        monitor.start()
        onDispose {
            monitor.stop()
            onBlackoutStopped()
        }
    }
    val breath = rememberInfiniteTransition()
    val glowAlpha by breath.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    0f to FrostbiteGlow.copy(alpha = glowAlpha),
                    0.55f to FrostbiteMid,
                    1f to FrostbiteBase,
                ),
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "The slate is clear. Lock your phone.",
            color = Color.White,
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = Lockout.formatCountdown(remainingMs),
            color = Color.White,
            fontSize = 40.sp,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}

@Preview
@Composable
private fun BlackoutScreenPreview() {
    BlackoutScreen(
        remainingMs = 2 * 60 * 60 * 1000L,
        humEnabled = false,
        onBlackoutStarted = {},
        onBlackoutStopped = {},
        onPocketCovered = {},
    )
}
