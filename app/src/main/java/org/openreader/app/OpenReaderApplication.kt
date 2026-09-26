package org.openreader.app

import android.app.Application
import org.openreader.feature.downloader.ModelDownloader

class OpenReaderApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    fun downloader(): ModelDownloader = container.modelDownloader
}
