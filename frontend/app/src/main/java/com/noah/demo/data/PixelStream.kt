package com.noah.demo.data

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

/** Width and height of the pixel canvas the course server paints. */
const val GRID_SIZE = 16

/** One cell repaint: `{"x":7,"y":13,"color":"#3a2a1a"}`. */
data class PixelUpdate(val x: Int, val y: Int, val color: Int)

sealed interface PixelEvent {
    data object Connected : PixelEvent
    data class Painted(val update: PixelUpdate) : PixelEvent
    data class Disconnected(val reason: String) : PixelEvent
}

/** Derives the relay's websocket URL from the configured HTTP base URL. */
fun pixelStreamUrl(apiBaseUrl: String): String =
    apiBaseUrl.trimEnd('/')
        .replaceFirst("https://", "wss://")
        .replaceFirst("http://", "ws://") + "/ws/pixels"

/**
 * Streams pixel updates from our own back-end relay.
 *
 * Malformed frames are skipped rather than terminating the stream: one bad frame
 * from upstream should not blank the screen for the rest of the session.
 */
fun pixelStream(url: String): Flow<PixelEvent> = callbackFlow {
    val client = OkHttpClient()

    val socket = client.newWebSocket(
        Request.Builder().url(url).build(),
        object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                trySend(PixelEvent.Connected)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parsePixel(text)?.let { trySend(PixelEvent.Painted(it)) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                trySend(PixelEvent.Disconnected(t.message ?: t.javaClass.simpleName))
                close()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                trySend(PixelEvent.Disconnected("closed ($code)"))
                close()
            }
        },
    )

    awaitClose { socket.cancel() }
}

private fun parsePixel(text: String): PixelUpdate? = runCatching {
    val json = JSONObject(text)
    val x = json.getInt("x")
    val y = json.getInt("y")
    if (x !in 0 until GRID_SIZE || y !in 0 until GRID_SIZE) return null
    PixelUpdate(x, y, parseHexColor(json.getString("color")) ?: return null)
}.getOrNull()

/** `#RRGGBB` to an opaque ARGB int. */
private fun parseHexColor(hex: String): Int? {
    if (hex.length != 7 || hex[0] != '#') return null
    val rgb = hex.substring(1).toLongOrNull(radix = 16) ?: return null
    return (0xFF000000L or rgb).toInt()
}
