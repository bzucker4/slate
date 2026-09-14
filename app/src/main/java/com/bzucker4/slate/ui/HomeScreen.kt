package com.bzucker4.slate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bzucker4.slate.ui.theme.SlateTheme

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
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
        Button(
            onClick = { /* Placeholder: session start is not wired yet. */ },
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text("Begin")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SlateTheme {
        HomeScreen()
    }
}
