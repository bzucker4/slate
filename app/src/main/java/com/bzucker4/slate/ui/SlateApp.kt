package com.bzucker4.slate.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    SlateTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (uiState.isLockedOutActive) {
                Color.Black
            } else {
                MaterialTheme.colorScheme.surface
            },
        ) {
            when {
                !uiState.storeLoaded -> Unit
                uiState.isLockedOutActive -> {
                    BlackoutScreen(remainingMs = uiState.remainingMs)
                }
                else -> {
                    HomeScreen(
                        selectedOption = uiState.selectedOption,
                        onSelectDuration = viewModel::selectDuration,
                        onBegin = viewModel::begin,
                    )
                }
            }
        }
    }
}
