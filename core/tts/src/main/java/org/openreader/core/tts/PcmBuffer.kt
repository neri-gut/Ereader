package org.openreader.core.tts

data class PcmBuffer(
    val paragraphIndex: Int,
    val text: String,
    val pcm16: ShortArray,
    val sampleRate: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PcmBuffer) return false
        return paragraphIndex == other.paragraphIndex &&
            text == other.text &&
            sampleRate == other.sampleRate &&
            pcm16.contentEquals(other.pcm16)
    }

    override fun hashCode(): Int {
        var result = paragraphIndex
        result = 31 * result + text.hashCode()
        result = 31 * result + pcm16.contentHashCode()
        result = 31 * result + sampleRate
        return result
    }
}
