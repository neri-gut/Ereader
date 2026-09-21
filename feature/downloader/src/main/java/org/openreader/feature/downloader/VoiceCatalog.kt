package org.openreader.feature.downloader

data class VoicePack(
    val id: String,
    val displayName: String,
    val languageCode: String,
    val archiveUrl: String,
    val onnxFileName: String,
    val onnxSha256: String,
    val tokensFileName: String = "tokens.txt"
)

object VoiceCatalog {
    val piperVoices: List<VoicePack> = listOf(
        VoicePack(
            id = "es_ES-carlfm-x_low",
            displayName = "Español (Carlfm)",
            languageCode = "es-ES",
            archiveUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-es_ES-carlfm-x_low.tar.bz2",
            onnxFileName = "es_ES-carlfm-x_low.onnx",
            onnxSha256 = "8a68b21d6133886129f057d0c568d342db4890113495f84d2e25bbcb7b1a37ce"
        ),
        VoicePack(
            id = "en_US-amy-low",
            displayName = "English (Amy)",
            languageCode = "en-US",
            archiveUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-amy-low.tar.bz2",
            onnxFileName = "en_US-amy-low.onnx",
            onnxSha256 = "8275b02c37c4ce6483b26a91704807e6de0c3f0bf8068e5d3b3598ab0da4253e"
        )
    )

    fun byId(id: String): VoicePack? = piperVoices.find { it.id == id }
}
