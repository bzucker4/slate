package com.bzucker4.slate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bzucker4.slate.ui.theme.SlateTheme

@Composable
fun SettingsScreen(
    humEnabled: Boolean,
    onHumEnabledChange: (Boolean) -> Unit,
    onEmergencyExit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        TextButton(onClick = onBack) {
            Text("Back")
        }
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text("Blackout hum")
                Text(
                    text = "Very quiet tone during lockout. Mutes automatically in a pocket.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Switch(
                checked = humEnabled,
                onCheckedChange = onHumEnabledChange,
            )
        }
        Text(
            text = "Emergency Exit is only available here, before Begin. It is hidden during blackout.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 40.dp),
        )
        Button(
            onClick = onEmergencyExit,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text("Emergency Exit")
        }
        Text(
            text = "Clears the persisted lockout flag for testing.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    SlateTheme {
        SettingsScreen(
            humEnabled = true,
            onHumEnabledChange = {},
            onEmergencyExit = {},
            onBack = {},
        )
    }
}
