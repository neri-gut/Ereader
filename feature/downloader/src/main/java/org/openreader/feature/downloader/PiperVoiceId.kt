package org.openreader.feature.downloader

import org.openreader.core.model.VoiceGender

object PiperVoiceId {
    private val PATTERN = Regex("^[a-z]{2,3}_[A-Z]{2}-[A-Za-z0-9_]+-(low|medium|high)$")
    private const val BASE =
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"

    fun matches(id: String): Boolean = PATTERN.matches(id) && !id.contains("..")

    fun parse(raw: String, displayName: String = "", gender: VoiceGender = VoiceGender.UNKNOWN): VoicePack? {
        val id = raw.trim()
        if (!matches(id)) return null
        val locale = id.substringBefore('-')
        val language = locale.replace('_', '-')
        val quality = id.substringAfterLast('-')
        val url = "$BASE/vits-piper-$id.tar.bz2"
        if (!isAllowedArchive(url)) return null
        val name = displayName.trim().ifBlank { id.replace('_', ' ') }
        return VoicePack(
            id = id,
            displayName = name,
            languageCode = language,
            gender = gender,
            quality = quality,
            archiveUrl = url,
            onnxFileName = "$id.onnx"
        )
    }

    fun isAllowedArchive(url: String): Boolean {
        if (url.isBlank()) return true
        if (url.contains("..") || url.contains("://") && !url.startsWith("https://")) return false
        return url.startsWith("$BASE/vits-piper-") && url.endsWith(".tar.bz2")
    }
}
