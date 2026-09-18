package com.noah.demo.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Everything Button 1 shows that only the back-end can answer. */
data class ServerInfo(
    val serverIp: String,
    val serverTime: String,
    val clientIp: String,
    val ownerFirstName: String,
    val ownerLastName: String,
) {
    val ownerName: String get() = "$ownerFirstName $ownerLastName"
}

/**
 * Reads the M1 server-info APIs. Uses HttpURLConnection to stay dependency-free,
 * matching the template's existing health check.
 */
object BackendClient {
    private const val TIMEOUT_MS = 5_000

    /**
     * Fetches all four endpoints concurrently. The server time is captured at
     * whatever moment the server handles the call, so issuing them together keeps
     * the displayed server and client clocks close enough to compare.
     */
    suspend fun fetchServerInfo(apiBaseUrl: String): ServerInfo = coroutineScope {
        val base = apiBaseUrl.trimEnd('/')

        val serverIp = async(Dispatchers.IO) { getJson("$base/api/server-ip") }
        val serverTime = async(Dispatchers.IO) { getJson("$base/api/server-time") }
        val clientIp = async(Dispatchers.IO) { getJson("$base/api/client-ip") }
        val name = async(Dispatchers.IO) { getJson("$base/api/name") }

        ServerInfo(
            serverIp = serverIp.await().getString("ip"),
            serverTime = serverTime.await().getString("time"),
            clientIp = clientIp.await().getString("ip"),
            ownerFirstName = name.await().getString("firstName"),
            ownerLastName = name.await().getString("lastName"),
        )
    }

    private suspend fun getJson(url: String): JSONObject = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
        }
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                error("HTTP $code from $url")
            }
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }
}
