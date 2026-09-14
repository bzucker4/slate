package com.bzucker4.slate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bzucker4.slate.lockout.Lockout
import com.bzucker4.slate.lockout.ProximityMonitor

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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
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
