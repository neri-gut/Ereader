package org.openreader.feature.downloader

import org.openreader.core.model.VoiceGender

data class VoicePack(
    val id: String,
    val displayName: String,
    val languageCode: String,
    val gender: VoiceGender,
    val archiveUrl: String,
    val onnxFileName: String,
    val onnxSha256: String = "",
    val tokensFileName: String = "tokens.txt"
)

object VoiceCatalog {
    private const val BASE =
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"

    val piperVoices: List<VoicePack> = listOf(
        pack("es_ES-sharvard-medium", "Español (Sharvard)", "es-ES", VoiceGender.MIXED),
        pack("es_ES-davefx-medium", "Español (DaveFX)", "es-ES", VoiceGender.MALE),
        pack("es_ES-carlfm-x_low", "Español (Carlfm, ligera)", "es-ES", VoiceGender.MALE),
        pack("es_MX-ald-medium", "Español MX (Ald)", "es-MX", VoiceGender.MALE),
        pack("en_US-amy-low", "English US (Amy)", "en-US", VoiceGender.FEMALE),
        pack("en_US-lessac-medium", "English US (Lessac)", "en-US", VoiceGender.FEMALE),
        pack("en_US-joe-medium", "English US (Joe)", "en-US", VoiceGender.MALE),
        pack("en_GB-alba-medium", "English UK (Alba)", "en-GB", VoiceGender.FEMALE),
        pack("pt_BR-faber-medium", "Português (Faber)", "pt-BR", VoiceGender.MALE),
        pack("fr_FR-siwis-medium", "Français (Siwis)", "fr-FR", VoiceGender.FEMALE),
        pack("it_IT-riccardo-x_low", "Italiano (Riccardo)", "it-IT", VoiceGender.MALE),
        pack("de_DE-thorsten-medium", "Deutsch (Thorsten)", "de-DE", VoiceGender.MALE)
    )

    fun byId(id: String): VoicePack? = piperVoices.find { it.id == id }

    private fun pack(
        id: String,
        name: String,
        language: String,
        gender: VoiceGender
    ): VoicePack = VoicePack(
        id = id,
        displayName = name,
        languageCode = language,
        gender = gender,
        archiveUrl = "$BASE/vits-piper-$id.tar.bz2",
        onnxFileName = "$id.onnx"
    )
}
