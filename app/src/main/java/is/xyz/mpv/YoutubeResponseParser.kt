package `is`.xyz.mpv

import org.json.JSONArray
import org.json.JSONObject

object YoutubeResponseParser {
    private val qualityMap = mapOf(
        "160" to "144p", "278" to "144p", "330" to "144p", "394" to "144p", "694" to "144p",
        "133" to "240p", "242" to "240p", "331" to "240p", "395" to "240p", "695" to "240p",
        "134" to "360p", "243" to "360p", "332" to "360p", "396" to "360p", "696" to "360p",
        "135" to "480p", "244" to "480p", "333" to "480p", "397" to "480p", "697" to "480p",
        "136" to "720p", "247" to "720p", "298" to "720p", "302" to "720p", "334" to "720p", "398" to "720p", "698" to "720p",
        "137" to "1080p", "299" to "1080p", "248" to "1080p", "303" to "1080p", "335" to "1080p", "399" to "1080p", "699" to "1080p",
        "264" to "1440p", "271" to "1440p", "304" to "1440p", "308" to "1440p", "336" to "1440p", "400" to "1440p", "700" to "1440p",
        "266" to "2160p", "305" to "2160p", "313" to "2160p", "315" to "2160p", "337" to "2160p", "401" to "2160p", "701" to "2160p",
        "138" to "4320p", "272" to "4320p", "402" to "4320p", "571" to "4320p"
    )

    @JvmStatic
    fun parseResponse(jsonResponse: String): YoutubeData {
        val json = JSONObject(jsonResponse)
        val playabilityStatus = json.getJSONObject("playabilityStatus")

        if (playabilityStatus.getString("status") != "OK") {
            throw Exception(playabilityStatus.optString("reason", "Video unavailable"))
        }

        val streamingData = json.getJSONObject("streamingData")
        val videoDetails = json.getJSONObject("videoDetails")
        val title = videoDetails.getString("title").trim()

        val formats = streamingData.optJSONArray("formats") ?: JSONArray()
        val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats") ?: JSONArray()
        val allFormats = formats.toList() + adaptiveFormats.toList()

        val videoStreams = mutableMapOf<String, VideoStream>()
        var bestAudioStream: AudioStream? = null

        for (format in allFormats) {
            if (!format.has("url") || !format.has("bitrate")) continue

            val bitrate = format.getInt("bitrate")
            val url = format.getString("url")
            val isVideo = format.has("height") && !format.has("audioQuality")
            val isAudio = format.has("audioQuality") && !format.has("height")

            when {
                isVideo -> {
                    val itag = format.getString("itag")
                    val height = format.getInt("height")
                    val fps = format.optInt("fps", 0)
                    val qualityLabel = qualityMap[itag] ?: "${height}p"
                    val quality = if (fps > 30) "${qualityLabel}${fps}" else qualityLabel

                    val existing = videoStreams[quality]
                    if (existing == null || bitrate > existing.bitrate) {
                        videoStreams[quality] = VideoStream(quality, url, bitrate)
                    }
                }
                isAudio -> {
                    if (bestAudioStream == null || bitrate > bestAudioStream.bitrate) {
                        bestAudioStream = AudioStream(url, bitrate)
                    }
                }
            }
        }

        if (videoStreams.isEmpty()) throw Exception("No video streams found")
        val finalAudioStream = bestAudioStream ?: throw Exception("No audio stream found")
        val sortedVideos = videoStreams.values.sortedByDescending { it.bitrate }

        return YoutubeData(title, sortedVideos, finalAudioStream)
    }

    private fun JSONArray.toList(): List<JSONObject> =
        (0 until length()).map { getJSONObject(it) }
}