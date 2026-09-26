package org.openreader.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openreader.core.model.AudioState
import org.openreader.core.model.TTSConfig
import org.openreader.core.model.TTSEngineType

@Composable
fun TtsControls(
    audioState: AudioState,
    config: TTSConfig,
    voiceLabel: String,
    palette: ThemeColors,
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
    var rateOpen by remember { mutableStateOf(false) }
    val tint = palette.text
    val engineName = when (config.engineType) {
        TTSEngineType.SYSTEM -> "Sistema"
        TTSEngineType.SHERPA_ONNX_PIPER -> "Neuronal"
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.background.copy(alpha = 0.94f))
            .navigationBarsPadding()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Anterior", tint = tint)
            }
            IconButton(
                onClick = {
                    when {
                        playing -> onPause()
                        paused -> onResume()
                        else -> onPlay()
                    }
                },
                modifier = Modifier.semantics {
                    contentDescription = if (playing) "Pausa" else "Leer"
                }
            ) {
                if (playing) {
                    Text("II", color = tint, fontSize = 16.sp)
                } else {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Leer", tint = tint)
                }
            }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Siguiente", tint = tint)
            }
            Text(
                text = "$engineName · $voiceLabel",
                color = tint,
                fontSize = 14.sp,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenVoices)
                    .padding(horizontal = 4.dp)
            )
            Text(
                text = "${"%.1f".format(config.speechRate)}×",
                color = tint,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable { rateOpen = !rateOpen }
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            )
        }
        if (rateOpen) {
            Slider(
                value = config.speechRate,
                onValueChange = onRateChange,
                valueRange = TTSConfig.MIN_SPEECH_RATE..TTSConfig.MAX_SPEECH_RATE,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        if (audioState is AudioState.Error) {
            Text(
                audioState.message,
                color = tint,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}
