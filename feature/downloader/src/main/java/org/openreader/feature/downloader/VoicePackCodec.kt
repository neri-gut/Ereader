package org.openreader.feature.downloader

import org.openreader.core.model.VoiceGender

internal object VoicePackCodec {
    fun encode(packs: List<VoicePack>): String {
        val body = packs.joinToString(",") { pack ->
            buildString {
                append('{')
                field("id", pack.id)
                field("displayName", pack.displayName)
                field("languageCode", pack.languageCode)
                field("gender", pack.gender.name)
                field("quality", pack.quality)
                field("archiveUrl", pack.archiveUrl)
                field("onnxFileName", pack.onnxFileName)
                append("\"speakerCount\":").append(pack.speakerCount)
                append('}')
            }
        }
        return "[$body]"
    }

    fun decode(raw: String): List<VoicePack> {
        val text = raw.trim()
        if (text.isEmpty() || text == "[]" || text == "{}") return emptyList()
        if (!text.startsWith("[")) return emptyList()
        val objects = text.removePrefix("[").removeSuffix("]").split("},{")
        return objects.mapNotNull { piece ->
            val body = piece.trim().removePrefix("{").removeSuffix("}")
            if (body.isBlank()) return@mapNotNull null
            val id = value(body, "id") ?: return@mapNotNull null
            if (id.contains("..")) return@mapNotNull null
            val archive = value(body, "archiveUrl").orEmpty()
            if (!PiperVoiceId.isAllowedArchive(archive)) return@mapNotNull null
            val gender = runCatching { VoiceGender.valueOf(value(body, "gender") ?: "UNKNOWN") }
                .getOrDefault(VoiceGender.UNKNOWN)
            VoicePack(
                id = id,
                displayName = value(body, "displayName") ?: id,
                languageCode = value(body, "languageCode") ?: "und",
                gender = gender,
                quality = value(body, "quality").orEmpty(),
                archiveUrl = archive,
                onnxFileName = value(body, "onnxFileName") ?: "$id.onnx",
                speakerCount = value(body, "speakerCount")?.toIntOrNull() ?: 1
            )
        }
    }

    private fun StringBuilder.field(name: String, value: String) {
        append('"').append(name).append("\":").append(quote(value)).append(',')
    }

    private fun quote(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun value(body: String, key: String): String? {
        val marker = "\"$key\":"
        val start = body.indexOf(marker)
        if (start < 0) return null
        var index = start + marker.length
        while (index < body.length && body[index].isWhitespace()) index++
        if (index >= body.length) return null
        if (body[index] != '"') {
            val end = body.indexOfAny(charArrayOf(',', '}'), index).let { if (it < 0) body.length else it }
            return body.substring(index, end).trim()
        }
        index++
        val out = StringBuilder()
        while (index < body.length) {
            val char = body[index]
            if (char == '\\' && index + 1 < body.length) {
                out.append(body[index + 1])
                index += 2
                continue
            }
            if (char == '"') return out.toString()
            out.append(char)
            index++
        }
        return null
    }
}
