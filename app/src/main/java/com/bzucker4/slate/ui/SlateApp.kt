package com.bzucker4.slate.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bzucker4.slate.ui.theme.SlateTheme

@Composable
fun SlateApp(
    viewModel: SlateViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }
    SlateTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (uiState.isLockedOutActive || uiState.scratching) {
                Color.Black
            } else {
                MaterialTheme.colorScheme.surface
            },
        ) {
            when {
                !uiState.storeLoaded -> Unit
                uiState.isLockedOutActive -> {
                    BlackoutScreen(
                        remainingMs = uiState.remainingMs,
                        humEnabled = uiState.humEnabled,
                        onBlackoutStarted = viewModel::onBlackoutStarted,
                        onBlackoutStopped = viewModel::onBlackoutStopped,
                        onPocketCovered = viewModel::onPocketCovered,
                    )
                }
                uiState.scratching -> {
                    ScratchScreen(
                        onCleared = viewModel::onScratchCleared,
                        onScrubMove = viewModel::onScrubMove,
                        onScrubStop = viewModel::onScrubStop,
                        onDissolveStart = viewModel::onDissolveStart,
                    )
                }
                showSettings -> {
                    SettingsScreen(
                        humEnabled = uiState.humEnabled,
                        onHumEnabledChange = viewModel::setHumEnabled,
                        onEmergencyExit = {
                            viewModel.emergencyExit()
                            showSettings = false
                        },
                        onBack = { showSettings = false },
                    )
                }
                else -> {
                    HomeScreen(
                        selectedOption = uiState.selectedOption,
                        onSelectDuration = viewModel::selectDuration,
                        onBegin = viewModel::begin,
                        showTip = !uiState.tipDismissed,
                        onDismissTip = viewModel::dismissTip,
                        onOpenSettings = { showSettings = true },
                    )
                }
            }
        }
    }
}
