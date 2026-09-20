/**
 * Configuración activa para el motor de voz.
 */
data class TTSConfig(
    val selectedVoiceId: String,
    val engineType: TTSEngineType = TTSEngineType.SHERPA_ONNX_PIPER,
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f
)