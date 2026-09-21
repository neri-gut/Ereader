package org.openreader.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.security.MessageDigest

class DocumentIdHasherTest {

    @Test
    fun sameContentProducesSameHash() {
        val payload = "OpenReader PDF fixture".repeat(100).toByteArray()
        val a = DocumentIdHasher.hashBlocking(ByteArrayInputStream(payload))
        val b = DocumentIdHasher.hashBlocking(ByteArrayInputStream(payload))
        assertEquals(a, b)
        assertEquals(64, a.length)
    }

    @Test
    fun hashIgnoresBytesAfterEightMegabytes() {
        val prefix = ByteArray(DocumentIdHasher.MAX_BYTES) { 7 }
        val withTail = prefix + byteArrayOf(1, 2, 3, 4)
        val withoutTail = prefix + byteArrayOf(9, 9, 9, 9)
        val hashA = DocumentIdHasher.hashBlocking(ByteArrayInputStream(withTail))
        val hashB = DocumentIdHasher.hashBlocking(ByteArrayInputStream(withoutTail))
        assertEquals(hashA, hashB)
    }

    @Test
    fun differentPrefixProducesDifferentHash() {
        val a = ByteArray(1024) { 1 }
        val b = ByteArray(1024) { 2 }
        assertNotEquals(
            DocumentIdHasher.hashBlocking(ByteArrayInputStream(a)),
            DocumentIdHasher.hashBlocking(ByteArrayInputStream(b))
        )
    }

    @Test
    fun differentFileSizeProducesDifferentHash() {
        val payload = ByteArray(2048) { 3 }
        val small = DocumentIdHasher.hashBlocking(ByteArrayInputStream(payload), fileSize = 2048)
        val large = DocumentIdHasher.hashBlocking(ByteArrayInputStream(payload), fileSize = 4096)
        assertNotEquals(small, large)
    }

    @Test
    fun matchesRawSha256ForSmallPayload() {
        val payload = byteArrayOf(10, 20, 30, 40, 50)
        val expected = MessageDigest.getInstance("SHA-256")
            .digest(payload)
            .joinToString("") { "%02x".format(it) }
        assertEquals(expected, DocumentIdHasher.hashBlocking(ByteArrayInputStream(payload)))
    }
}
