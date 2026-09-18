package com.noah.demo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The three M1 entry points. Each is independent of the others, so nothing here
 * is gated on another button having been pressed first.
 */
@Composable
fun HomeScreen(
    onLogin: () -> Unit,
    onLiveUpdates: () -> Unit,
    onTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "CPEN 321 M1",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(40.dp))

        HomeButton("Login + Server", onLogin)
        Spacer(Modifier.height(16.dp))
        HomeButton("Live Updates", onLiveUpdates)
        Spacer(Modifier.height(16.dp))
        HomeButton("Timer", onTimer)
    }
}

@Composable
private fun HomeButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}
