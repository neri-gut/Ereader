package org.openreader.core.model

/**
 * Configuración activa para el motor de voz.
 */
data class TTSConfig(
    val selectedVoiceId: String = SYSTEM_VOICE_ID,
    val engineType: TTSEngineType = TTSEngineType.SYSTEM,
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f
) {
    companion object {
        const val SYSTEM_VOICE_ID = "system_default"
        const val MIN_SPEECH_RATE = 0.5f
        const val MAX_SPEECH_RATE = 3.0f
    }
}
