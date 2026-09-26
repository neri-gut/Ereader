package org.openreader.core.tts

interface AudioSink {
    suspend fun play(
        buffer: PcmBuffer,
        onProgress: (startChar: Int, endChar: Int) -> Unit
    )

    fun pause()
    fun resumePlayback()
    fun stop()
    fun release()
}
