package org.openreader.core.model

/**
 * Representa un párrafo de texto plano extraído del PDF.
 *
 * @property id Índice consecutivo único del párrafo dentro del documento.
 * @property text Contenido textual limpio (sin saltos de línea duros ni guiones de separación).
 * @property pageNumber Página original del PDF de la cual fue extraído (para referencia).
 */
data class ParagraphData(
    val id: Int,
    val text: String,
    val pageNumber: Int
)
