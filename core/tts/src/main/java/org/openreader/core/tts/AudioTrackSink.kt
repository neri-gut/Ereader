package org.openreader.core.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

class AudioTrackSink : AudioSink {
    private var track: AudioTrack? = null
    @Volatile
    private var paused: Boolean = false
    @Volatile
    private var stopped: Boolean = false

    override suspend fun play(
        buffer: PcmBuffer,
        onProgress: (startChar: Int, endChar: Int) -> Unit
    ) {
        stopped = false
        paused = false
        val minBuf = AudioTrack.getMinBufferSize(
            buffer.sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val created = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(buffer.sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuf, buffer.pcm16.size * 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track?.release()
        track = created
        created.play()
        val headAtStart = created.playbackHeadPosition

        val chunk = ShortArray(2048)
        var offset = 0
        val total = buffer.pcm16.size
        var lastKick = 0L
        var failed = false
        while (offset < total && !stopped && !failed && coroutineContext.isActive) {
            while (paused && !stopped) {
                delay(16)
            }
            if (stopped) break
            val remaining = total - offset
            val count = minOf(chunk.size, remaining)
            buffer.pcm16.copyInto(chunk, 0, offset, offset + count)
            val written = created.write(chunk, 0, count, AudioTrack.WRITE_NON_BLOCKING)
            if (written > 0) {
                offset += written
                notifyWord(buffer.text, offset, total, onProgress)
            } else if (written == AudioTrack.ERROR_DEAD_OBJECT) {
                failed = true
            } else if (!stopped) {
                lastKick = keepTrackPlaying(created, lastKick)
                delay(20)
            }
        }
        if (!failed && !stopped && coroutineContext.isActive) {
            waitUntilDrained(created, headAtStart, offset)
        }
        created.stop()
        created.release()
        if (track === created) track = null
        if (!coroutineContext.isActive) throw CancellationException()
    }

    override fun pause() {
        paused = true
        track?.pause()
    }

    override fun resumePlayback() {
        paused = false
        track?.play()
    }

    override fun stop() {
        stopped = true
        paused = false
        runCatching { track?.pause() }
        runCatching { track?.flush() }
    }

    override fun release() {
        stop()
        track?.release()
        track = null
    }

    private fun notifyWord(
        text: String,
        samplesWritten: Int,
        totalSamples: Int,
        onProgress: (Int, Int) -> Unit
    ) {
        if (text.isEmpty() || totalSamples == 0) return
        val frac = samplesWritten.toFloat() / totalSamples.toFloat()
        val index = (frac * text.length).roundToInt().coerceIn(0, text.length)
        val start = text.lastIndexOf(' ', index - 1).let { if (it < 0) 0 else it + 1 }
        var end = text.indexOf(' ', index)
        if (end < 0) end = text.length
        onProgress(start, end)
    }

    /**
     * Con la pantalla apagada el sistema puede dejar el track en pausa.
     * Si aquí se interpreta eso como fin de buffer, se tira el audio que
     * aún no sonó. Se espera a los frames escritos y se vuelve a dar play.
     */
    private suspend fun waitUntilDrained(created: AudioTrack, headAtStart: Int, framesWritten: Int) {
        if (framesWritten <= 0) return
        var lastKick = 0L
        while (!stopped && coroutineContext.isActive) {
            if (paused) {
                delay(16)
                continue
            }
            if (playedSince(headAtStart, created.playbackHeadPosition) >= framesWritten) break
            lastKick = keepTrackPlaying(created, lastKick)
            delay(16)
        }
    }

    private fun keepTrackPlaying(created: AudioTrack, lastKick: Long): Long {
        if (paused || stopped) return lastKick
        if (created.playState == AudioTrack.PLAYSTATE_PLAYING) return lastKick
        val now = SystemClock.uptimeMillis()
        if (now - lastKick < 200L) return lastKick
        Log.i(TAG, "El sistema pausó el audio; se reanuda el track")
        runCatching { created.play() }
        return now
    }

    private fun playedSince(start: Int, now: Int): Long {
        return (now.toLong() - start.toLong()) and 0xffffffffL
    }

    private companion object {
        const val TAG = "OpenReaderAudio"
    }
}
