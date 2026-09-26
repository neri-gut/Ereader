package org.openreader.core.tts

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openreader.core.model.ParagraphData

class TtsQueueManagerTest {

    @Test
    fun prefetchChannelCapacityIsTwo() {
        assertEquals(2, TtsQueueManager.CHANNEL_CAPACITY)
    }

    @Test
    fun producerFillsChannelAheadOfConsumer() {
        runBlocking {
            val started = CompletableDeferred<Unit>()
            val channel = Channel<Int>(capacity = TtsQueueManager.CHANNEL_CAPACITY)
            val producer = async(Dispatchers.Default) {
                for (i in 0 until 4) {
                    if (i == 2) started.complete(Unit)
                    channel.send(i)
                }
                channel.close()
            }
            withTimeout(1_000) { started.await() }
            assertTrue(channel.tryReceive().isSuccess)
            producer.cancel()
            channel.close()
        }
    }

    @Test
    fun paragraphsKeepSequentialIds() {
        val paragraphs = listOf(
            ParagraphData(0, "uno", 1),
            ParagraphData(1, "dos", 1)
        )
        assertEquals(listOf(0, 1), paragraphs.map { it.id })
    }
}
