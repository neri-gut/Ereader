package org.openreader.feature.downloader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openreader.core.model.TTSEngineType
import org.openreader.core.model.VoiceGender
import org.openreader.core.model.VoiceModel

class VoiceCatalogMenuTest {

    @Test
    fun piperIdBuildsSherpaUrl() {
        val pack = PiperVoiceId.parse("es_ES-mls_9972-low")
        assertEquals("es-ES", pack?.languageCode)
        assertEquals(
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-es_ES-mls_9972-low.tar.bz2",
            pack?.archiveUrl
        )
    }

    @Test
    fun rejectsDotDotAndForeignHost() {
        assertNull(PiperVoiceId.parse("es_ES-../evil-low"))
        assertNull(PiperVoiceId.parse("not a voice"))
        assertTrue(
            !PiperVoiceId.isAllowedArchive("https://example.com/vits-piper-es_ES-mls_9972-low.tar.bz2")
        )
    }

    @Test
    fun emptyJsonAddsNothing() {
        assertTrue(VoicePackCodec.decode("").isEmpty())
        assertTrue(VoicePackCodec.decode("[]").isEmpty())
        assertTrue(VoicePackCodec.decode("{}").isEmpty())
    }

    @Test
    fun codecDropsForeignUrl() {
        val raw = """[{"id":"es_ES-mls_9972-low","displayName":"MLS","languageCode":"es-ES","gender":"FEMALE","quality":"low","archiveUrl":"https://evil.example/pack.tar.bz2","onnxFileName":"es_ES-mls_9972-low.onnx","speakerCount":1}]"""
        assertTrue(VoicePackCodec.decode(raw).isEmpty())
    }

    @Test
    fun browseStartsAtSourceAndKeepsEnginesApart() {
        val neural = listOf(voice("a", "Alfa", "es-ES", VoiceGender.FEMALE))
        val system = listOf(
            voice("system:es", "Local", "es-ES", VoiceGender.UNKNOWN)
                .copy(engineType = TTSEngineType.SYSTEM, isDownloaded = true)
        )
        val root = browseVoices(neural, system, source = null, language = null, gender = null, activeId = "")
        val step = root.step as BrowseStep.Sources
        assertEquals(listOf("Neuronal", "Sistema"), step.choices.map { it.label })
        val neuralLang = browseVoices(neural, system, VoiceSource.NEURAL, null, null, "")
        val voices = (neuralLang.step as BrowseStep.Voices).voices
        assertEquals(listOf("a"), voices.map { it.id })
    }

    @Test
    fun languageThenGenderThenTraits() {
        val neural = listOf(
            voice("es_ES-a", "Ana", "es-ES", VoiceGender.FEMALE),
            voice("es_MX-b", "Bruno", "es-MX", VoiceGender.MALE),
            voice("en_US-c", "Cara", "en-US", VoiceGender.FEMALE)
        )
        val languages = browseVoices(neural, emptyList(), VoiceSource.NEURAL, null, null, "")
        assertEquals(listOf("en", "es"), (languages.step as BrowseStep.Languages).choices.map { it.id })
        val genders = browseVoices(neural, emptyList(), VoiceSource.NEURAL, "es", null, "")
        assertEquals(listOf("Mujer", "Hombre"), (genders.step as BrowseStep.Genders).choices.map { it.label })
        val leaf = browseVoices(neural, emptyList(), VoiceSource.NEURAL, "es", VoiceGender.FEMALE, "es_ES-a")
        val voices = (leaf.step as BrowseStep.Voices).voices
        assertEquals(listOf("es_ES-a"), voices.map { it.id })
        assertEquals("España · Por descargar", voiceTraits(voices.single()))
        assertEquals(listOf("Neuronal", "Español", "Mujer"), leaf.trail)
    }

    @Test
    fun singleGenderSkipsToTheName() {
        val neural = listOf(
            voice("a", "Ana", "es-ES", VoiceGender.FEMALE, downloaded = true),
            voice("b", "Bea", "es-MX", VoiceGender.FEMALE).copy(speakerCount = 2)
        )
        val step = browseVoices(neural, emptyList(), VoiceSource.NEURAL, null, null, "b")
        val voices = (step.step as BrowseStep.Voices).voices
        assertEquals(listOf("b", "a"), voices.map { it.id })
        assertEquals("México · 2 hablantes · Por descargar", voiceTraits(voices.first()))
    }

    @Test
    fun sampleControlIsPerSpeaker() {
        assertTrue(samplePlaying("sharvard#1", "sharvard", 1))
        assertTrue(!samplePlaying("sharvard#1", "sharvard", 0))
        assertEquals(1, sampleSpeaker("sharvard#1", "sharvard"))
        assertEquals(null, sampleSpeaker("otra#0", "sharvard"))
    }

    @Test
    fun backSkipsALevelThatHadOnlyOneChoice() {
        val neural = listOf(
            voice("a", "Ana", "es-ES", VoiceGender.FEMALE),
            voice("b", "Bea", "en-US", VoiceGender.FEMALE)
        )
        val back = popBrowse(
            neural,
            emptyList(),
            BrowseSelection(VoiceSource.NEURAL, "es", VoiceGender.FEMALE)
        )
        assertEquals(VoiceSource.NEURAL, back.source)
        assertNull(back.language)
        assertNull(back.gender)
    }

    private fun voice(
        id: String,
        name: String,
        locale: String,
        gender: VoiceGender,
        downloaded: Boolean = false
    ) = VoiceModel(
        id = id,
        name = name,
        languageCode = locale,
        engineType = TTSEngineType.SHERPA_ONNX_PIPER,
        gender = gender,
        isDownloaded = downloaded
    )
}
