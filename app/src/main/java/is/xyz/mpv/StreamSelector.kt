package `is`.xyz.mpv

import android.content.Context
import android.preference.PreferenceManager
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object StreamSelector {
    suspend fun selectStream(context: Context, data: YoutubeData): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val quality = prefs.getString("yt_quality", "")
        val audioOnly = prefs.getBoolean("yt_audio_only", false)

        if (audioOnly) {
            return -1
        }

        return when (quality) {
            "highest" -> 0
            "lowest" -> data.videos.size - 1
            "" -> showQualityDialog(context, data)
            else -> {
                val preferredIndex = data.videos.indexOfFirst {
                    it.quality.lowercase().contains(quality!!.lowercase())
                }
                if (preferredIndex != -1) preferredIndex else 0
            }
        }
    }

    private suspend fun showQualityDialog(context: Context, data: YoutubeData): Int {
        return suspendCancellableCoroutine { continuation ->
            val options = data.videos.map { "${it.quality} (${it.bitrate / 1000} kbps)" } + "Audio Only"
            var selectedIndex = 0

            val dialog = AlertDialog.Builder(context).apply {
                setTitle("Select Quality for: ${data.title}")
                setSingleChoiceItems(options.toTypedArray(), 0) { _, which ->
                    selectedIndex = which
                }
                setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    val result = if (selectedIndex == options.size - 1) -1 else selectedIndex
                    if (continuation.isActive) continuation.resume(result)
                }
                setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    if (continuation.isActive) continuation.resume(0)
                }
                setOnCancelListener {
                    if (continuation.isActive) continuation.resume(0)
                }
            }.create()

            continuation.invokeOnCancellation {
                dialog.dismiss()
            }

            dialog.show()
        }
    }
}