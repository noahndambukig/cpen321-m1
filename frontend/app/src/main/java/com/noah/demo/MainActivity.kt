package com.noah.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.noah.demo.data.BackendClient
import com.noah.demo.data.GoogleAuth
import com.noah.demo.data.SignInResult
import com.noah.demo.data.SignedInUser
import com.noah.demo.data.PixelEvent
import com.noah.demo.data.currentClientTime
import com.noah.demo.data.pixelStream
import com.noah.demo.data.pixelStreamUrl
import com.noah.demo.ui.HomeScreen
import com.noah.demo.ui.PixelGridScreen
import com.noah.demo.ui.PixelGridState
import com.noah.demo.ui.ServerInfoScreen
import com.noah.demo.ui.ServerInfoState
import com.noah.demo.ui.TimerRoute
import com.noah.demo.ui.theme.NoahDemoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A pause this long from the stream means the next image is starting. */
private const val NEW_IMAGE_GAP_MS = 2_000L

/** Backoff bounds for re-opening the pixel stream after it drops. */
private const val RECONNECT_BASE_MS = 1_000L
private const val RECONNECT_MAX_MS = 15_000L

/** Which of the three M1 features is on screen. */
private enum class Screen { Home, ServerInfo, LiveUpdates, Timer }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoahDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppRoot(
                        apiBaseUrl = BuildConfig.API_BASE_URL,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRoot(apiBaseUrl: String, modifier: Modifier = Modifier) {
    var screen by remember { mutableStateOf(Screen.Home) }

    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

    when (screen) {
        Screen.Home -> HomeScreen(
            onLogin = { screen = Screen.ServerInfo },
            onLiveUpdates = { screen = Screen.LiveUpdates },
            onTimer = { screen = Screen.Timer },
            modifier = modifier,
        )

        Screen.ServerInfo -> ServerInfoRoute(apiBaseUrl, modifier)

        Screen.LiveUpdates -> LiveUpdatesRoute(apiBaseUrl, modifier)

        // Not `modifier`: the Scaffold's inset padding would leave the
        // wallpaper-tinted background showing behind the system bars, framing the
        // CRT screen in a stray colour. CrtSurface insets its own content.
        Screen.Timer -> TimerRoute()
    }
}

@Composable
private fun ServerInfoRoute(apiBaseUrl: String, modifier: Modifier = Modifier) {
    var state by remember { mutableStateOf<ServerInfoState>(ServerInfoState.Loading) }
    var attempt by remember { mutableIntStateOf(0) }
    var user by remember { mutableStateOf<SignedInUser?>(null) }
    var signInError by remember { mutableStateOf<String?>(null) }
    var isSigningIn by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(attempt) {
        state = ServerInfoState.Loading
        state = runCatching { BackendClient.fetchServerInfo(apiBaseUrl) }.fold(
            onSuccess = { ServerInfoState.Ready(it, currentClientTime()) },
            onFailure = {
                ServerInfoState.Failed(
                    "$apiBaseUrl: ${it.message ?: it.javaClass.simpleName}",
                )
            },
        )
    }

    ServerInfoScreen(
        state = state,
        signedInUser = user?.displayName,
        signInError = signInError,
        isSigningIn = isSigningIn,
        onSignIn = {
            isSigningIn = true
            signInError = null
            scope.launch {
                when (val result = GoogleAuth.signIn(context, BuildConfig.GOOGLE_CLIENT_ID)) {
                    is SignInResult.Success -> user = result.user
                    SignInResult.Cancelled -> signInError = null
                    is SignInResult.Failed -> signInError = result.message
                }
                isSigningIn = false
            }
        },
        onRetry = { attempt++ },
        modifier = modifier,
    )
}

/**
 * Button 2. Pixels arrive one at a time from our relay and are painted as they
 * land. The course server pauses ~5s between images, so a gap that long is taken
 * as "next image starting" and clears the canvas.
 */
@Composable
private fun LiveUpdatesRoute(apiBaseUrl: String, modifier: Modifier = Modifier) {
    val grid = remember { PixelGridState() }
    var status by remember { mutableStateOf("Connecting...") }

    LaunchedEffect(apiBaseUrl) {
        val url = pixelStreamUrl(apiBaseUrl)
        var lastPaintedAt = 0L
        var painted = 0
        var backoff = RECONNECT_BASE_MS

        // The stream ends whenever the relay drops (server restart, network blip).
        // Without this loop the screen would stay dead until the user navigated away.
        while (true) {
            pixelStream(url).collect { event ->
                when (event) {
                    PixelEvent.Connected -> {
                        backoff = RECONNECT_BASE_MS
                        status = "Connected. Waiting for pixels"
                    }

                    is PixelEvent.Painted -> {
                        val now = System.currentTimeMillis()
                        if (lastPaintedAt != 0L && now - lastPaintedAt > NEW_IMAGE_GAP_MS) {
                            grid.clear()
                            painted = 0
                        }
                        lastPaintedAt = now
                        grid.paint(event.update)
                        painted++
                        status = "Painting: $painted pixels this image"
                    }

                    is PixelEvent.Disconnected -> status = "Disconnected: ${event.reason}"
                }
            }

            status = "Reconnecting..."
            delay(backoff)
            backoff = (backoff * 2).coerceAtMost(RECONNECT_MAX_MS)
            // A fresh connection joins mid-image, so start from a blank canvas.
            lastPaintedAt = 0L
            painted = 0
            grid.clear()
        }
    }

    PixelGridScreen(grid = grid, status = status, modifier = modifier)
}
