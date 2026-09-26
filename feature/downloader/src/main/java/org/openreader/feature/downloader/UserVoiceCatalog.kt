package org.openreader.feature.downloader

import java.io.File

class UserVoiceCatalog(filesDir: File) {
    private val file = File(filesDir, "user-voices.json")

    fun load(): List<VoicePack> {
        if (!file.isFile) return emptyList()
        return runCatching { VoicePackCodec.decode(file.readText()) }.getOrDefault(emptyList())
    }

    fun save(packs: List<VoicePack>) {
        file.parentFile?.mkdirs()
        file.writeText(VoicePackCodec.encode(packs))
        VoiceCatalog.setUserPacks(packs)
    }

    fun upsert(pack: VoicePack) {
        val next = load().filterNot { it.id == pack.id } + pack
        save(next)
    }

    fun remove(id: String): Boolean {
        val current = load()
        if (current.none { it.id == id }) return false
        save(current.filterNot { it.id == id })
        return true
    }
}
