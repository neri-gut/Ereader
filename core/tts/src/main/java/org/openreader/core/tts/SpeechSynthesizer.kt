package org.openreader.core.tts

interface SpeechSynthesizer {
    suspend fun synthesize(paragraphIndex: Int, text: String, speed: Float): PcmBuffer
    fun release() {}
}
