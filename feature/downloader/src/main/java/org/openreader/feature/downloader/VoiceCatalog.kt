package org.openreader.feature.downloader

import org.openreader.core.model.VoiceGender

data class VoicePack(
    val id: String,
    val displayName: String,
    val languageCode: String,
    val gender: VoiceGender,
    val quality: String,
    val archiveUrl: String,
    val onnxFileName: String,
    val onnxSha256: String = "",
    val tokensFileName: String = "tokens.txt",
    val speakerCount: Int = 1,
    val speakerLabels: List<String> = emptyList()
) {
    fun sampleUrl(speaker: Int = 0): String {
        if (!PiperVoiceId.matches(id)) return ""
        val qualityPart = id.substringAfterLast('-')
        val withoutQuality = id.removeSuffix("-$qualityPart")
        val locale = withoutQuality.substringBefore('-')
        val name = withoutQuality.substringAfter('-')
        val lang = locale.substringBefore('_').lowercase()
        val sid = speaker.coerceIn(0, (speakerCount - 1).coerceAtLeast(0))
        return "https://huggingface.co/rhasspy/piper-voices/resolve/main/" +
            "$lang/$locale/$name/$qualityPart/samples/speaker_$sid.mp3"
    }
}

object VoiceCatalog {
    private const val BASE =
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"

    @Volatile
    private var userPacks: List<VoicePack> = emptyList()

    fun setUserPacks(packs: List<VoicePack>) {
        userPacks = packs
    }

    fun all(): List<VoicePack> {
        val overrides = userPacks.map { it.id }.toSet()
        return userPacks + piperVoices.filter { it.id !in overrides }
    }

    val piperVoices: List<VoicePack> = listOf(
        pack("es_AR-daniela-high", "Español AR (Daniela)", "es-AR", VoiceGender.FEMALE, "alta"),
        pack("es_MX-claude-high", "Español MX (Claude)", "es-MX", VoiceGender.MALE, "alta"),
        pack("es_ES-glados-medium", "Español (GLaDOS)", "es-ES", VoiceGender.FEMALE, "media"),
        pack(
            "es_ES-sharvard-medium",
            "Español (Sharvard)",
            "es-ES",
            VoiceGender.MIXED,
            "media",
            speakerCount = 2,
            speakerLabels = listOf("Hablante 1", "Hablante 2")
        ),
        pack("es_ES-davefx-medium", "Español (DaveFX)", "es-ES", VoiceGender.MALE, "media"),
        pack("es_MX-ald-medium", "Español MX (Ald)", "es-MX", VoiceGender.MALE, "media"),
        pack("en_US-lessac-high", "English US (Lessac)", "en-US", VoiceGender.FEMALE, "alta"),
        pack("en_US-amy-medium", "English US (Amy)", "en-US", VoiceGender.FEMALE, "media"),
        pack("en_US-hfc_female-medium", "English US (HFC Female)", "en-US", VoiceGender.FEMALE, "media"),
        pack("en_US-lessac-medium", "English US (Lessac)", "en-US", VoiceGender.FEMALE, "media"),
        pack("en_US-ryan-medium", "English US (Ryan)", "en-US", VoiceGender.MALE, "media"),
        pack("en_US-joe-medium", "English US (Joe)", "en-US", VoiceGender.MALE, "media"),
        pack("en_GB-alba-medium", "English UK (Alba)", "en-GB", VoiceGender.FEMALE, "media"),
        pack("pt_BR-faber-medium", "Português (Faber)", "pt-BR", VoiceGender.MALE, "media"),
        pack("fr_FR-siwis-medium", "Français (Siwis)", "fr-FR", VoiceGender.FEMALE, "media"),
        pack("it_IT-paola-medium", "Italiano (Paola)", "it-IT", VoiceGender.FEMALE, "media"),
        pack("de_DE-thorsten-medium", "Deutsch (Thorsten)", "de-DE", VoiceGender.MALE, "media")
    )

    fun byId(id: String): VoicePack? = all().find { it.id == id }

    private fun pack(
        id: String,
        name: String,
        language: String,
        gender: VoiceGender,
        quality: String,
        speakerCount: Int = 1,
        speakerLabels: List<String> = emptyList()
    ): VoicePack = VoicePack(
        id = id,
        displayName = "$name · $quality",
        languageCode = language,
        gender = gender,
        quality = quality,
        archiveUrl = "$BASE/vits-piper-$id.tar.bz2",
        onnxFileName = "$id.onnx",
        speakerCount = speakerCount,
        speakerLabels = speakerLabels
    )
}
