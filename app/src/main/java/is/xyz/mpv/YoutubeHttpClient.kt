package `is`.xyz.mpv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

object YoutubeHttpClient {
    private const val API_URL = "https://www.youtube.com/youtubei/v1/player?key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    private const val YOUTUBE_ORIGIN = "https://www.youtube.com"

    private sealed class YoutubeClient(
        val clientName: String,
        val clientVersion: String,
        val clientNameId: String,
        val userAgent: String,
        val deviceModel: String? = null
    ) {
        data object IOS : YoutubeClient(
            clientName = "IOS",
            clientVersion = "17.13.3",
            clientNameId = "5",
            userAgent = "com.google.ios.youtube/17.13.3 (iPhone14,3; U; CPU OS 15_6 like Mac OS X)",
            deviceModel = "iPhone14,3"
        )

        data object Android : YoutubeClient(
            clientName = "ANDROID",
            clientVersion = "19.50.42",
            clientNameId = "3",
            userAgent = "com.google.android.youtube/19.50.42 (Linux; U; Android 13) gzip"
        )
    }

    @JvmStatic
    suspend fun fetchYoutubeData(videoId: String, useIos: Boolean): String {
        return withContext(Dispatchers.IO) {
            val client = if (useIos) YoutubeClient.IOS else YoutubeClient.Android
            val url = URL(API_URL)
            val connection = url.openConnection() as HttpURLConnection
            val requestBody = buildRequestBody(videoId, client)

            try {
                connection.apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("User-Agent", client.userAgent)
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("X-Youtube-Client-Name", client.clientNameId)
                    setRequestProperty("X-Youtube-Client-Version", client.clientVersion)
                    setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                    setRequestProperty("Origin", YOUTUBE_ORIGIN)
                    setRequestProperty("Referer", "$YOUTUBE_ORIGIN/watch?v=$videoId")
                }

                DataOutputStream(connection.outputStream).use { it.writeBytes(requestBody) }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    throw Exception("HTTP error: $responseCode")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun buildRequestBody(videoId: String, client: YoutubeClient): String {
        val clientObject = JSONObject().apply {
            put("hl", "en")
            put("gl", "US")
            put("clientName", client.clientName)
            put("clientVersion", client.clientVersion)
            client.deviceModel?.let { put("deviceModel", it) }
        }

        val contextObject = JSONObject().apply {
            put("client", clientObject)
            put("user", JSONObject().put("lockedSafetyMode", false))
        }

        return JSONObject().apply {
            put("context", contextObject)
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }.toString()
    }
}