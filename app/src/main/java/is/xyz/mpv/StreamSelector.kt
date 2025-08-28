package `is`.xyz.mpv

import android.content.Context
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object StreamSelector {
    const val AUDIO_ONLY_INDEX = -2 // Sentinel value for audio-only selection

    suspend fun selectStream(context: Context, data: YoutubeData): Int {
        return suspendCancellableCoroutine { continuation ->
            val videoQualities = data.videos.map { it.quality }
            val choices = (videoQualities + "Audio Only").toTypedArray()
            var selectedIndex = 0 // Default to the first video quality

            val dialog = AlertDialog.Builder(context)
                .setTitle("Select Stream Quality")
                .setSingleChoiceItems(choices, selectedIndex) { _, which ->
                    selectedIndex = which
                }
                .setPositiveButton("Play") { dialog, _ ->
                    dialog.dismiss()
                    if (continuation.isActive) {
                        val result = if (selectedIndex == videoQualities.size) {
                            AUDIO_ONLY_INDEX // "Audio Only" was selected
                        } else {
                            selectedIndex // Index matches the video list
                        }
                        continuation.resume(result)
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    if (continuation.isActive) {
                        continuation.resume(-1) // Cancellation
                    }
                }
                .setOnCancelListener {
                    if (continuation.isActive) {
                        continuation.resume(-1) // Cancellation
                    }
                }
                .create()

            continuation.invokeOnCancellation {
                dialog.dismiss()
            }

            dialog.show()
        }
    }
}