package org.openreader.core.tts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log

/**
 * Servicio de reproducción. Mantiene la sesión, la notificación y un
 * wake lock parcial para que la voz siga con la pantalla apagada.
 */
class ReadingPlaybackService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val playing = intent?.getBooleanExtra(EXTRA_PLAYING, true) ?: true
        val title = intent?.getStringExtra(EXTRA_TITLE)?.ifBlank { "OpenReader" } ?: "OpenReader"
        val stopping = intent?.action == ACTION_STOP
        if (!stopping) {
            ensureChannel()
            promote(buildNotification(title, playing))
            if (playing && ReadingForeground.desiredRunning) holdWake() else releaseWake()
        }
        if (!ReadingForeground.desiredRunning) {
            shutdown()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        releaseWake()
        super.onDestroy()
    }

    override fun onTimeout(startId: Int) {
        shutdown()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        shutdown()
    }

    private fun shutdown() {
        releaseWake()
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    private fun promote(notification: Notification) {
        runCatching {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    ReadingForeground.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(ReadingForeground.NOTIFICATION_ID, notification)
            }
        }.onFailure { Log.e(TAG, "startForeground", it) }
    }

    private fun holdWake() {
        val existing = wakeLock
        val lock = existing ?: (getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OpenReader:reading")
            .also {
                it.setReferenceCounted(false)
                wakeLock = it
            })
        if (!lock.isHeld) lock.acquire()
    }

    private fun releaseWake() {
        val lock = wakeLock ?: return
        if (lock.isHeld) lock.release()
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lectura en voz alta",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Controles de la lectura mientras suena el audio"
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(title: String, playing: Boolean): Notification {
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reading)
            .setContentTitle(title)
            .setContentText(if (playing) "Leyendo" else "En pausa")
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_TRANSPORT)
        contentIntent()?.let { builder.setContentIntent(it) }
        val token = ReadingForeground.token
        if (token != null) {
            builder.setStyle(Notification.MediaStyle().setMediaSession(token))
        }
        return builder.build()
    }

    private fun contentIntent(): PendingIntent? {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        launch.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            this,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_STOP = "org.openreader.core.tts.STOP_READING"
        const val EXTRA_TITLE = "title"
        const val EXTRA_PLAYING = "playing"
        private const val CHANNEL_ID = "openreader_reading"
        private const val TAG = "OpenReaderAudio"
    }
}
