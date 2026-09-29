package org.openreader.core.tts

import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.util.Log

/**
 * Mantiene el proceso en primer plano mientras hay lectura.
 * Con la pantalla apagada, sin este servicio el sistema pausa el AudioTrack
 * y la síntesis del párrafo siguiente no vuelve hasta que el usuario reanuda.
 */
internal object ReadingForeground {
    const val NOTIFICATION_ID = 43

    @Volatile
    var token: MediaSession.Token? = null

    @Volatile
    var desiredRunning: Boolean = false

    @Volatile
    private var engaged: Boolean = false

    fun sync(context: Context, title: String, playing: Boolean, active: Boolean) {
        desiredRunning = active
        if (!active && !engaged) return
        val intent = Intent(context, ReadingPlaybackService::class.java)
            .putExtra(ReadingPlaybackService.EXTRA_TITLE, title)
            .putExtra(ReadingPlaybackService.EXTRA_PLAYING, playing)
        if (active) {
            engaged = true
            runCatching { context.startForegroundService(intent) }
                .onFailure { Log.e(TAG, "No se pudo sostener la lectura en primer plano", it) }
        } else {
            intent.action = ReadingPlaybackService.ACTION_STOP
            runCatching { context.startService(intent) }
        }
    }

    private const val TAG = "OpenReaderAudio"
}
