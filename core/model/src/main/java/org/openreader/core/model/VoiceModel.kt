package org.openreader.core.model

enum class TTSEngineType {
    SYSTEM,
    SHERPA_ONNX_PIPER
}

enum class VoiceGender {
    FEMALE,
    MALE,
    MIXED,
    UNKNOWN
}

/**
 * Representa un modelo de voz neuronal local o voz del sistema.
 */
data class VoiceModel(
    val id: String,
    val name: String,
    val languageCode: String,
    val engineType: TTSEngineType,
    val gender: VoiceGender = VoiceGender.UNKNOWN,
    val modelFileName: String? = null,
    val tokensFileName: String? = null,
    val isDownloaded: Boolean = false
)
