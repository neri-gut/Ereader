package org.openreader.feature.downloader

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

class VoiceSamplePlayer {
    private var player: MediaPlayer? = null
    var currentUrl: String? = null
        private set

    fun play(url: String, onError: (String) -> Unit = {}) {
        stop()
        currentUrl = url
        try {
            val created = MediaPlayer()
            player = created
            created.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            created.setDataSource(url)
            created.setOnCompletionListener { stop() }
            created.setOnErrorListener { _, _, extra ->
                Log.e(TAG, "Sample playback error extra=$extra")
                onError("No se pudo reproducir la muestra")
                stop()
                true
            }
            created.prepareAsync()
            created.setOnPreparedListener { it.start() }
        } catch (error: Exception) {
            onError(error.message ?: "No se pudo reproducir la muestra")
            stop()
        }
    }

    fun stop() {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
        currentUrl = null
    }

    private companion object {
        const val TAG = "VoiceSample"
    }
}
