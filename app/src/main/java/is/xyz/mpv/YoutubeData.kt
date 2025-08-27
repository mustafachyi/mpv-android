package `is`.xyz.mpv

data class VideoStream(
    val quality: String,
    val url: String,
    val bitrate: Int
)

data class AudioStream(
    val url: String,
    val bitrate: Int
)

data class YoutubeData(
    val title: String,
    val videos: List<VideoStream>,
    val audio: AudioStream
)