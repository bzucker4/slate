package com.bzucker4.slate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bzucker4.slate.lockout.DurationOption
import com.bzucker4.slate.ui.theme.SlateTheme

@Composable
fun HomeScreen(
    selectedOption: DurationOption,
    onSelectDuration: (DurationOption) -> Unit,
    onBegin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Slate",
            style = MaterialTheme.typography.headlineLarge,
        )
        FlowRow(
            modifier = Modifier.padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DurationChip(
                label = "30m",
                selected = selectedOption == DurationOption.ThirtyMinutes,
                onClick = { onSelectDuration(DurationOption.ThirtyMinutes) },
            )
            DurationChip(
                label = "2h",
                selected = selectedOption == DurationOption.TwoHours,
                onClick = { onSelectDuration(DurationOption.TwoHours) },
            )
            DurationChip(
                label = "Until morning",
                selected = selectedOption == DurationOption.UntilMorning,
                onClick = { onSelectDuration(DurationOption.UntilMorning) },
            )
        }
        Button(
            onClick = onBegin,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text("Begin")
        }
    }
}

@Composable
private fun DurationChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SlateTheme {
        HomeScreen(
            selectedOption = DurationOption.TwoHours,
            onSelectDuration = {},
            onBegin = {},
        )
    }
}
