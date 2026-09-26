package org.openreader.feature.downloader

import org.openreader.core.model.TTSEngineType
import org.openreader.core.model.VoiceGender
import org.openreader.core.model.VoiceModel

enum class VoiceSource { NEURAL, SYSTEM }

data class BrowseChoice(val id: String, val label: String, val count: Int)

sealed class BrowseStep {
    data class Sources(val choices: List<BrowseChoice>) : BrowseStep()
    data class Languages(val choices: List<BrowseChoice>) : BrowseStep()
    data class Genders(val choices: List<BrowseChoice>) : BrowseStep()
    data class Voices(val voices: List<VoiceModel>) : BrowseStep()
}

data class BrowseView(val step: BrowseStep, val trail: List<String>)

data class BrowseSelection(
    val source: VoiceSource? = null,
    val language: String? = null,
    val gender: VoiceGender? = null
)

fun popBrowse(
    neural: List<VoiceModel>,
    system: List<VoiceModel>,
    selection: BrowseSelection
): BrowseSelection {
    val pool = when (selection.source) {
        VoiceSource.NEURAL -> neural
        VoiceSource.SYSTEM -> system
        null -> return BrowseSelection()
    }
    if (selection.gender != null) {
        val inLang = pool.filter { languageKey(it.languageCode) == selection.language }
        if (inLang.map { it.gender }.distinct().size > 1) {
            return selection.copy(gender = null)
        }
    }
    if (selection.language != null && pool.map { languageKey(it.languageCode) }.distinct().size > 1) {
        return BrowseSelection(source = selection.source)
    }
    return BrowseSelection()
}

fun browseVoices(
    neural: List<VoiceModel>,
    system: List<VoiceModel>,
    source: VoiceSource?,
    language: String?,
    gender: VoiceGender?,
    activeId: String
): BrowseView {
    if (source == null) {
        val choices = listOfNotNull(
            neural.takeIf { it.isNotEmpty() }?.let { BrowseChoice("neural", "Neuronal", it.size) },
            system.takeIf { it.isNotEmpty() }?.let { BrowseChoice("system", "Sistema", it.size) }
        )
        return BrowseView(BrowseStep.Sources(choices), emptyList())
    }
    val pool = if (source == VoiceSource.NEURAL) neural else system
    val sourceLabel = if (source == VoiceSource.NEURAL) "Neuronal" else "Sistema"
    val lang = language ?: pool.map { languageKey(it.languageCode) }.distinct().singleOrNull()
    if (lang == null) {
        return BrowseView(BrowseStep.Languages(languageChoices(pool)), listOf(sourceLabel))
    }
    val inLang = pool.filter { languageKey(it.languageCode) == lang }
    val langLabel = languageLabel(lang)
    val resolvedGender = gender ?: inLang.map { it.gender }.distinct().singleOrNull()
    if (resolvedGender == null) {
        return BrowseView(
            BrowseStep.Genders(genderChoices(inLang)),
            listOf(sourceLabel, langLabel)
        )
    }
    val voices = inLang.filter { it.gender == resolvedGender }
        .sortedWith(
            compareByDescending<VoiceModel> { it.id == activeId }
                .thenBy { it.name.lowercase() }
        )
    return BrowseView(
        BrowseStep.Voices(voices),
        listOf(sourceLabel, langLabel, genderLabel(resolvedGender))
    )
}

fun languageKey(code: String): String =
    code.replace('_', '-').substringBefore('-').lowercase().ifBlank { "und" }

fun languageLabel(code: String): String = when (languageKey(code)) {
    "es" -> "Español"
    "en" -> "English"
    "pt" -> "Português"
    "fr" -> "Français"
    "it" -> "Italiano"
    "de" -> "Deutsch"
    "und" -> "Otro"
    else -> code.ifBlank { "Otro" }
}

fun genderLabel(gender: VoiceGender): String = when (gender) {
    VoiceGender.FEMALE -> "Mujer"
    VoiceGender.MALE -> "Hombre"
    VoiceGender.MIXED -> "Varias"
    VoiceGender.UNKNOWN -> "Sin marcar"
}

fun sampleKey(voiceId: String, speakerId: Int): String = "$voiceId#$speakerId"

fun samplePlaying(current: String?, voiceId: String, speakerId: Int): Boolean =
    current == sampleKey(voiceId, speakerId)

fun voiceSamplePlaying(current: String?, voiceId: String): Boolean =
    current?.substringBefore('#') == voiceId

fun sampleSpeaker(current: String?, voiceId: String): Int? {
    if (!voiceSamplePlaying(current, voiceId)) return null
    return current?.substringAfter('#')?.toIntOrNull()
}

fun canPreviewVoice(voice: VoiceModel): Boolean =
    voice.engineType != TTSEngineType.SYSTEM &&
        (voice.isDownloaded || PiperVoiceId.matches(voice.id))

fun voiceTraits(voice: VoiceModel): String {
    val parts = mutableListOf<String>()
    val region = regionLabel(voice.languageCode)
    if (region.isNotEmpty()) parts += region
    if (voice.speakerCount > 1) parts += "${voice.speakerCount} hablantes"
    if (voice.engineType == TTSEngineType.SHERPA_ONNX_PIPER) {
        parts += if (voice.isDownloaded) "Lista" else "Por descargar"
    }
    return parts.joinToString(" · ")
}

fun regionLabel(code: String): String {
    val region = code.replace('_', '-').split('-').getOrNull(1)?.uppercase().orEmpty()
    return when (region) {
        "ES" -> "España"
        "MX" -> "México"
        "AR" -> "Argentina"
        "US" -> "Estados Unidos"
        "GB" -> "Reino Unido"
        "BR" -> "Brasil"
        "FR" -> "Francia"
        "IT" -> "Italia"
        "DE" -> "Alemania"
        "PT" -> "Portugal"
        else -> region
    }
}

private fun languageChoices(voices: List<VoiceModel>): List<BrowseChoice> =
    voices.groupBy { languageKey(it.languageCode) }
        .map { (key, group) -> BrowseChoice(key, languageLabel(key), group.size) }
        .sortedBy { it.label.lowercase() }

private val genderOrder = listOf(
    VoiceGender.FEMALE,
    VoiceGender.MALE,
    VoiceGender.MIXED,
    VoiceGender.UNKNOWN
)

private fun genderChoices(voices: List<VoiceModel>): List<BrowseChoice> =
    genderOrder.mapNotNull { gender ->
        val count = voices.count { it.gender == gender }
        if (count == 0) null else BrowseChoice(gender.name, genderLabel(gender), count)
    }
