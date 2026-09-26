package org.openreader.core.tts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.openreader.core.model.AudioState
import java.util.concurrent.atomic.AtomicInteger

/**
 * Sesión multimedia y foco de audio para que el sistema, los audífonos
 * y el resto de apps vean la lectura como una reproducción.
 */
class ReadingMediaSession(
    context: Context,
    private val transport: Transport
) {
    interface Transport {
        fun onMediaPlay()
        fun onMediaPause()
        fun onMediaNext()
        fun onMediaPrevious()
        fun onMediaStop()
    }

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val notifications = appContext.getSystemService(NotificationManager::class.java)
    private val epoch = AtomicInteger(0)
    private val speechAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(speechAttributes)
        .setOnAudioFocusChangeListener { change -> onFocusChange(change) }
        .setWillPauseWhenDucked(false)
        .build()
    private val session = MediaSession(appContext, "OpenReader").apply {
        @Suppress("DEPRECATION")
        setFlags(
            MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        setCallback(object : MediaSession.Callback() {
            override fun onPlay() = transport.onMediaPlay()
            override fun onPause() = transport.onMediaPause()
            override fun onSkipToNext() = transport.onMediaNext()
            override fun onSkipToPrevious() = transport.onMediaPrevious()
            override fun onStop() = transport.onMediaStop()
        }, main)
    }

    private var title: String = "OpenReader"
    private var hasFocus: Boolean = false
    private var focusAcquiredAt: Long = 0L
    private var resumeAfterTransientLoss: Boolean = false
    private var shownMode: Int = MODE_IDLE

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lectura en voz alta",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Controles de la lectura mientras suena el audio"
            setSound(null, null)
        }
        notifications.createNotificationChannel(channel)
    }

    fun setTitle(value: String) {
        val next = value.removeSuffix(".pdf").ifBlank { "OpenReader" }
        main.post {
            title = next
            session.setMetadata(
                android.media.MediaMetadata.Builder()
                    .putString(android.media.MediaMetadata.METADATA_KEY_TITLE, title)
                    .putString(android.media.MediaMetadata.METADATA_KEY_ARTIST, "OpenReader")
                    .build()
            )
        }
    }

    fun tryAcquireFocus(): Boolean {
        acquireFocus()
        return hasFocus
    }

    fun onAudioState(state: AudioState) {
        val mode = when (state) {
            is AudioState.Playing, is AudioState.Synthesizing -> MODE_PLAYING
            is AudioState.Paused -> MODE_PAUSED
            else -> MODE_IDLE
        }
        val stamp = epoch.get()
        main.post {
            if (mode == MODE_IDLE && epoch.get() != stamp) return@post
            if (mode == shownMode) return@post
            shownMode = mode
            when (mode) {
                MODE_PLAYING -> {
                    epoch.incrementAndGet()
                    showPlaying()
                }
                MODE_PAUSED -> showPaused()
                else -> showIdle()
            }
        }
    }

    fun release() {
        main.post {
            abandonFocus()
            session.isActive = false
            notifications.cancel(NOTIFICATION_ID)
            session.release()
        }
    }

    private fun showPlaying() {
        resumeAfterTransientLoss = false
        acquireFocus()
        session.setPlaybackState(playbackState(PlaybackState.STATE_PLAYING))
        session.isActive = true
        notify(playing = true)
    }

    private fun showPaused() {
        if (!resumeAfterTransientLoss) abandonFocus()
        session.setPlaybackState(playbackState(PlaybackState.STATE_PAUSED))
        session.isActive = true
        notify(playing = false)
    }

    private fun showIdle() {
        resumeAfterTransientLoss = false
        abandonFocus()
        session.setPlaybackState(playbackState(PlaybackState.STATE_STOPPED))
        session.isActive = false
        notifications.cancel(NOTIFICATION_ID)
    }

    private fun onFocusChange(change: Int) {
        val justAcquired = SystemClock.uptimeMillis() - focusAcquiredAt < 800L
        if (justAcquired && change != AudioManager.AUDIOFOCUS_GAIN) return
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (resumeAfterTransientLoss) {
                    resumeAfterTransientLoss = false
                    transport.onMediaPlay()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeAfterTransientLoss = false
                hasFocus = false
                transport.onMediaPause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                resumeAfterTransientLoss = true
                transport.onMediaPause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> Unit
        }
    }

    private fun acquireFocus() {
        if (hasFocus) return
        val result = audioManager.requestAudioFocus(focusRequest)
        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (hasFocus) focusAcquiredAt = SystemClock.uptimeMillis()
    }

    private fun abandonFocus() {
        if (!hasFocus && !resumeAfterTransientLoss) {
            audioManager.abandonAudioFocusRequest(focusRequest)
            return
        }
        hasFocus = false
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    private fun playbackState(state: Int): PlaybackState {
        val actions = PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_SKIP_TO_NEXT or
            PlaybackState.ACTION_SKIP_TO_PREVIOUS or
            PlaybackState.ACTION_STOP
        return PlaybackState.Builder()
            .setActions(actions)
            .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, if (state == PlaybackState.STATE_PLAYING) 1f else 0f)
            .build()
    }

    private fun notify(playing: Boolean) {
        val notification = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reading)
            .setContentTitle(title)
            .setContentText(if (playing) "Leyendo" else "En pausa")
            .setOngoing(playing)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken))
            .build()
        runCatching { notifications.notify(NOTIFICATION_ID, notification) }
    }

    private companion object {
        const val CHANNEL_ID = "openreader_reading"
        const val NOTIFICATION_ID = 43
        const val MODE_IDLE = 0
        const val MODE_PLAYING = 1
        const val MODE_PAUSED = 2
    }
}
