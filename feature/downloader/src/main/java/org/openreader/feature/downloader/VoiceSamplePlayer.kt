package org.openreader.feature.downloader

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

class VoiceSamplePlayer {
    private var player: MediaPlayer? = null
    private var generation: Int = 0
    var currentUrl: String? = null
        private set

    fun play(url: String, onDone: () -> Unit = {}, onError: (String) -> Unit = {}) {
        stop()
        currentUrl = url
        val token = generation
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
            created.setOnCompletionListener {
                if (token != generation) return@setOnCompletionListener
                releasePlayer()
                onDone()
            }
            created.setOnErrorListener { _, _, extra ->
                Log.e(TAG, "Sample playback error extra=$extra")
                if (token == generation) {
                    releasePlayer()
                    onError("No se pudo reproducir la muestra")
                }
                true
            }
            created.prepareAsync()
            created.setOnPreparedListener { ready ->
                if (token == generation) ready.start()
            }
        } catch (error: Exception) {
            releasePlayer()
            onError(error.message ?: "No se pudo reproducir la muestra")
        }
    }

    fun stop() {
        generation++
        releasePlayer()
    }

    private fun releasePlayer() {
        val current = player
        player = null
        currentUrl = null
        current?.setOnCompletionListener(null)
        current?.setOnErrorListener(null)
        runCatching {
            current?.stop()
            current?.release()
        }
    }

    private companion object {
        const val TAG = "VoiceSample"
    }
}
