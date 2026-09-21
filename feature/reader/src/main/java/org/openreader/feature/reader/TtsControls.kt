package org.openreader.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
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
    neuralReady: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRateChange: (Float) -> Unit,
    onEngineChange: (TTSEngineType) -> Unit,
    onOpenVoices: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playing = audioState is AudioState.Playing || audioState is AudioState.Synthesizing
    val paused = audioState is AudioState.Paused
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onPrevious) { Text("Anterior") }
            when {
                playing -> FilledTonalButton(onClick = onPause) { Text("Pausa") }
                paused -> FilledTonalButton(onClick = onResume) { Text("Continuar") }
                else -> FilledTonalButton(onClick = onPlay) { Text("Play") }
            }
            TextButton(onClick = onNext) { Text("Siguiente") }
        }
        Text(
            text = "Velocidad ${"%.1f".format(config.speechRate)}x",
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Slider(
            value = config.speechRate,
            onValueChange = onRateChange,
            valueRange = TTSConfig.MIN_SPEECH_RATE..TTSConfig.MAX_SPEECH_RATE,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = { onEngineChange(TTSEngineType.SYSTEM) }) {
                Text(if (config.engineType == TTSEngineType.SYSTEM) "Sistema ✓" else "Sistema")
            }
            TextButton(onClick = { onEngineChange(TTSEngineType.SHERPA_ONNX_PIPER) }) {
                val label = if (neuralReady) "Neuronal ✓" else "Neuronal"
                Text(if (config.engineType == TTSEngineType.SHERPA_ONNX_PIPER) "$label · activa" else label)
            }
            TextButton(onClick = onOpenVoices) { Text("Voces") }
        }
        if (audioState is AudioState.Synthesizing) {
            Text("Preparando párrafo ${audioState.paragraphIndex + 1}…", modifier = Modifier.padding(8.dp))
        }
        if (audioState is AudioState.Error) {
            Text(audioState.message, modifier = Modifier.padding(8.dp))
        }
    }
}
