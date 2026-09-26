package org.openreader.core.model

sealed interface DownloadState {
    data object Idle : DownloadState

    data class InProgress(
        val voiceId: String,
        val bytesRead: Long,
        val totalBytes: Long
    ) : DownloadState

    data class Verifying(
        val voiceId: String
    ) : DownloadState

    data class Completed(
        val voiceId: String
    ) : DownloadState

    data class Failed(
        val voiceId: String,
        val message: String
    ) : DownloadState
}
