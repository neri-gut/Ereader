package org.openreader.core.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParagraphNormalizerTest {

    @Test
    fun dehyphenatesLineBreaks() {
        val joined = ParagraphNormalizer.dehyphenate("infor-", "mación")
        assertEquals("información", joined)
    }

    @Test
    fun stripsRepeatedHeadersAndPageNumbers() {
        val pages = listOf(
            PageText(1, "OpenReader\nIntroducción al texto continuo.\n\nSegundo párrafo de la página uno.\n1"),
            PageText(2, "OpenReader\nContinuación del relato con más contenido útil.\n2"),
            PageText(3, "OpenReader\nCierre del documento sin marcas de agua.\n3")
        )
        val paragraphs = ParagraphNormalizer.normalize(pages)
        assertTrue(paragraphs.none { it.text.contains("OpenReader") })
        assertTrue(paragraphs.none { it.text == "1" || it.text == "2" || it.text == "3" })
        assertEquals(paragraphs.indices.toList(), paragraphs.map { it.id })
        assertTrue(paragraphs.any { it.text.contains("Introducción") })
    }

    @Test
    fun joinsWrappedLinesInsideParagraph() {
        val joined = ParagraphNormalizer.joinLines(
            listOf(
                "Este es un párrafo que continúa",
                "en la línea siguiente sin punto."
            )
        )
        assertEquals(
            "Este es un párrafo que continúa en la línea siguiente sin punto.",
            joined
        )
    }

    @Test
    fun singleLineBreaksStayInTheSameParagraph() {
        val chunk = ParagraphNormalizer.processPage(
            "Primera oración termina aquí.\nSegunda oración empieza con mayúscula.\nY sigue el mismo párrafo.",
            emptySet(),
            ""
        )
        assertEquals(
            listOf("Primera oración termina aquí. Segunda oración empieza con mayúscula. Y sigue el mismo párrafo."),
            chunk.paragraphs
        )
    }

    @Test
    fun keepsParagraphsSeparatedByBlankLines() {
        val chunk = ParagraphNormalizer.processPage(
            "Uno dos tres.\n\nCuatro cinco seis.",
            emptySet(),
            ""
        )
        assertEquals(listOf("Uno dos tres.", "Cuatro cinco seis."), chunk.paragraphs)
        assertEquals("", chunk.carry)
    }

    @Test
    fun wrappedLinesStayInsideTheSameParagraph() {
        val chunk = ParagraphNormalizer.processPage(
            "Este es un párrafo que continúa\nen la línea siguiente sin punto final.",
            emptySet(),
            ""
        )
        assertEquals(
            listOf("Este es un párrafo que continúa en la línea siguiente sin punto final."),
            chunk.paragraphs
        )
        assertEquals("", chunk.carry)
    }

    @Test
    fun unfinishedParagraphIsCarriedToTheNextPage() {
        val chunk = ParagraphNormalizer.processPage(
            "La frase sigue\nsin terminar",
            emptySet(),
            ""
        )
        assertTrue(chunk.paragraphs.isEmpty())
        assertEquals("La frase sigue sin terminar", chunk.carry)
    }

    @Test
    fun keepsSentenceBoundariesAsSeparateLines() {
        val joined = ParagraphNormalizer.joinLines(
            listOf(
                "Primera oración termina aquí.",
                "Segunda oración empieza con mayúscula."
            )
        )
        assertEquals(
            "Primera oración termina aquí.\nSegunda oración empieza con mayúscula.",
            joined
        )
    }
}
