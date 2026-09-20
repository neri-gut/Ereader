package org.openreader.core.model

/**
 * Estados del reproductor TTS comunicados a la interfaz en tiempo real.
 */
sealed interface AudioState {
    data object Idle : AudioState

    data class Synthesizing(
        val paragraphIndex: Int
    ) : AudioState

    data class Playing(
        val paragraphIndex: Int,
        val startCharOffset: Int,
        val endCharOffset: Int
    ) : AudioState

    data class Paused(
        val paragraphIndex: Int
    ) : AudioState

    data class Error(
        val message: String
    ) : AudioState
}
