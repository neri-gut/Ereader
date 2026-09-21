package org.openreader.core.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.openreader.core.model.FontFamilyType
import org.openreader.core.model.ReaderTheme
import org.openreader.core.model.TTSConfig
import org.openreader.core.model.TTSEngineType
import org.openreader.core.model.ThemeType

private val Context.openReaderDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "openreader_prefs"
)

class PreferencesRepository(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val dataStore = context.applicationContext.openReaderDataStore

    val theme: Flow<ReaderTheme> = dataStore.data.map { prefs ->
        ReaderTheme(
            type = prefs[KEY_THEME]?.let { runCatching { ThemeType.valueOf(it) }.getOrNull() }
                ?: ThemeType.LIGHT,
            fontFamily = prefs[KEY_FONT]?.let { runCatching { FontFamilyType.valueOf(it) }.getOrNull() }
                ?: FontFamilyType.SANS_SERIF,
            fontSizeSp = (prefs[KEY_FONT_SIZE] ?: 18).coerceIn(
                ReaderTheme.MIN_FONT_SIZE_SP,
                ReaderTheme.MAX_FONT_SIZE_SP
            ),
            lineHeightMultiplier = (prefs[KEY_LINE_HEIGHT] ?: 1.4f).coerceIn(
                ReaderTheme.MIN_LINE_HEIGHT,
                ReaderTheme.MAX_LINE_HEIGHT
            ),
            maxContainerWidthRem = (prefs[KEY_MAX_WIDTH] ?: 44).coerceIn(
                ReaderTheme.MIN_WIDTH_REM,
                ReaderTheme.MAX_WIDTH_REM
            )
        )
    }

    val ttsConfig: Flow<TTSConfig> = dataStore.data.map { prefs ->
        TTSConfig(
            selectedVoiceId = prefs[KEY_VOICE_ID] ?: TTSConfig.SYSTEM_VOICE_ID,
            engineType = prefs[KEY_ENGINE]?.let { runCatching { TTSEngineType.valueOf(it) }.getOrNull() }
                ?: TTSEngineType.SYSTEM,
            speechRate = (prefs[KEY_SPEECH_RATE] ?: 1.0f).coerceIn(
                TTSConfig.MIN_SPEECH_RATE,
                TTSConfig.MAX_SPEECH_RATE
            ),
            pitch = prefs[KEY_PITCH] ?: 1.0f
        )
    }

    suspend fun saveTheme(theme: ReaderTheme) = withContext(ioDispatcher) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme.type.name
            prefs[KEY_FONT] = theme.fontFamily.name
            prefs[KEY_FONT_SIZE] = theme.fontSizeSp.coerceIn(
                ReaderTheme.MIN_FONT_SIZE_SP,
                ReaderTheme.MAX_FONT_SIZE_SP
            )
            prefs[KEY_LINE_HEIGHT] = theme.lineHeightMultiplier.coerceIn(
                ReaderTheme.MIN_LINE_HEIGHT,
                ReaderTheme.MAX_LINE_HEIGHT
            )
            prefs[KEY_MAX_WIDTH] = theme.maxContainerWidthRem.coerceIn(
                ReaderTheme.MIN_WIDTH_REM,
                ReaderTheme.MAX_WIDTH_REM
            )
        }
    }

    suspend fun saveTtsConfig(config: TTSConfig) = withContext(ioDispatcher) {
        dataStore.edit { prefs ->
            prefs[KEY_VOICE_ID] = config.selectedVoiceId
            prefs[KEY_ENGINE] = config.engineType.name
            prefs[KEY_SPEECH_RATE] = config.speechRate.coerceIn(
                TTSConfig.MIN_SPEECH_RATE,
                TTSConfig.MAX_SPEECH_RATE
            )
            prefs[KEY_PITCH] = config.pitch
        }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_type")
        val KEY_FONT = stringPreferencesKey("font_family")
        val KEY_FONT_SIZE = intPreferencesKey("font_size_sp")
        val KEY_LINE_HEIGHT = floatPreferencesKey("line_height")
        val KEY_MAX_WIDTH = intPreferencesKey("max_width_rem")
        val KEY_VOICE_ID = stringPreferencesKey("voice_id")
        val KEY_ENGINE = stringPreferencesKey("tts_engine")
        val KEY_SPEECH_RATE = floatPreferencesKey("speech_rate")
        val KEY_PITCH = floatPreferencesKey("pitch")
    }
}
