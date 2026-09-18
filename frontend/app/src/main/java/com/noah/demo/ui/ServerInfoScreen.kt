package com.noah.demo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.noah.demo.data.ServerInfo

/** What Button 1's screen is currently showing. */
sealed interface ServerInfoState {
    data object Loading : ServerInfoState
    data class Ready(val info: ServerInfo, val clientTime: String) : ServerInfoState
    data class Failed(val message: String) : ServerInfoState
}

/**
 * Button 1's result screen. Six values, each clearly labelled. M1 deducts marks
 * for a screen that is messy or hard to read.
 */
@Composable
fun ServerInfoScreen(
    state: ServerInfoState,
    signedInUser: String?,
    signInError: String?,
    isSigningIn: Boolean,
    onSignIn: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(
            text = "Login + Server",
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(Modifier.height(20.dp))

        when (state) {
            ServerInfoState.Loading -> LoadingBody()
            is ServerInfoState.Failed -> FailedBody(state.message, onRetry)
            is ServerInfoState.Ready -> ReadyBody(
                state = state,
                signedInUser = signedInUser,
                signInError = signInError,
                isSigningIn = isSigningIn,
                onSignIn = onSignIn,
                onRetry = onRetry,
            )
        }
    }
}

@Composable
private fun LoadingBody() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Contacting the server...", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FailedBody(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Could not reach the server",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun ReadyBody(
    state: ServerInfoState.Ready,
    signedInUser: String?,
    signInError: String?,
    isSigningIn: Boolean,
    onSignIn: () -> Unit,
    onRetry: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InfoRow("Server IP address", state.info.serverIp)
            InfoRow("Client IP address", state.info.clientIp)

            HorizontalDivider()

            InfoRow("Server local time", state.info.serverTime)
            InfoRow("Client local time", state.clientTime)

            HorizontalDivider()

            InfoRow("Name", state.info.ownerName)
            InfoRow("Signed in as", signedInUser ?: "Not signed in")
        }
    }

    Spacer(Modifier.height(20.dp))

    if (signedInUser == null) {
        Button(onClick = onSignIn, enabled = !isSigningIn) {
            Text(if (isSigningIn) "Signing in..." else "Sign in with Google")
        }
        if (signInError != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = signInError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(12.dp))
    }

    OutlinedButton(onClick = onRetry) { Text("Refresh") }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
        )
    }
}
