package org.openreader.feature.downloader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.openreader.core.model.DownloadState

class ModelDownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val voiceId = intent?.getStringExtra(EXTRA_VOICE_ID) ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        createChannel()
        val notification = buildNotification("Descargando voz neuronal", 0, 0)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        val downloader = downloaderFromApp()
        scope.launch {
            try {
                val progressJob = launch {
                    downloader.state.collect { state ->
                        when (state) {
                            is DownloadState.InProgress -> {
                                val max = if (state.totalBytes > 0) 100 else 0
                                val progress = if (state.totalBytes > 0) {
                                    ((state.bytesRead * 100) / state.totalBytes).toInt()
                                } else 0
                                notify("Descargando ${state.voiceId}", progress, max)
                            }
                            is DownloadState.Verifying -> notify("Comprobando archivos del modelo", 0, 0)
                            is DownloadState.Completed -> notify("Voz instalada", 100, 100)
                            is DownloadState.Failed -> notify("Error: ${state.message}", 0, 0)
                            DownloadState.Idle -> Unit
                        }
                    }
                }
                try {
                    downloader.download(voiceId)
                } catch (error: Throwable) {
                    android.util.Log.e("OpenReaderDownload", "Service download failed", error)
                }
                progressJob.cancel()
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun downloaderFromApp(): ModelDownloader {
        val app = application
        val getter = app.javaClass.methods.first { it.name == "downloader" }
        return getter.invoke(app) as ModelDownloader
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Descarga de modelos",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun notify(text: String, progress: Int, max: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text, progress, max))
    }

    private fun buildNotification(text: String, progress: Int, max: Int): Notification {
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("OpenReader")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
        if (max > 0) {
            builder.setProgress(max, progress, false)
        } else {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }

    companion object {
        const val EXTRA_VOICE_ID = "voice_id"
        private const val CHANNEL_ID = "model_download"
        private const val NOTIFICATION_ID = 42

        fun start(context: Context, voiceId: String) {
            val intent = Intent(context, ModelDownloadService::class.java)
                .putExtra(EXTRA_VOICE_ID, voiceId)
            context.startForegroundService(intent)
        }
    }
}
