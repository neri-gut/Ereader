package org.openreader.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.openreader.core.model.AudioState
import org.openreader.core.model.TTSConfig
import org.openreader.core.model.TTSEngineType

@Composable
fun TtsControls(
    audioState: AudioState,
    config: TTSConfig,
    voiceLabel: String,
    paragraphIndex: Int,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRateChange: (Float) -> Unit,
    onOpenVoices: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playing = audioState is AudioState.Playing || audioState is AudioState.Synthesizing
    val paused = audioState is AudioState.Paused
    val engineName = when (config.engineType) {
        TTSEngineType.SYSTEM -> "Sistema"
        TTSEngineType.SHERPA_ONNX_PIPER -> "Neuronal"
    }
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(
            text = buildString {
                append("$engineName · $voiceLabel")
                if (config.engineType == TTSEngineType.SHERPA_ONNX_PIPER && config.speakerId > 0) {
                    append(" · hablante ${config.speakerId + 1}")
                }
            },
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = "Párrafo ${paragraphIndex + 1} (toca un párrafo para empezar ahí)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onPrevious) { Text("Anterior") }
            when {
                playing -> FilledTonalButton(onClick = onPause) { Text("Pausa") }
                paused -> FilledTonalButton(onClick = onResume) { Text("Continuar") }
                else -> FilledTonalButton(onClick = onPlay) { Text("Leer") }
            }
            TextButton(onClick = onNext) { Text("Siguiente") }
        }
        Text("Velocidad ${"%.1f".format(config.speechRate)}×")
        Slider(
            value = config.speechRate,
            onValueChange = onRateChange,
            valueRange = TTSConfig.MIN_SPEECH_RATE..TTSConfig.MAX_SPEECH_RATE
        )
        TextButton(onClick = onOpenVoices) { Text("Cambiar o descargar voces") }
        if (audioState is AudioState.Synthesizing) {
            Text("Preparando párrafo ${audioState.paragraphIndex + 1}…")
        }
        if (audioState is AudioState.Error) {
            Text(audioState.message, color = MaterialTheme.colorScheme.error)
        }
    }
}
