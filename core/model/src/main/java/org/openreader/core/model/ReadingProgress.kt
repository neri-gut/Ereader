package org.openreader.core.model

/**
 * Estado de progreso de lectura guardado por archivo PDF.
 *
 * @property fileHash Hash SHA-256 único del archivo.
 * @property fileName Nombre visible del archivo para la UI.
 * @property paragraphIndex Índice del párrafo actual activo.
 * @property charOffset Desplazamiento de carácter dentro del párrafo actual.
 * @property totalParagraphs Total de párrafos contenidos en el documento.
 * @property lastReadTimestamp Timestamp en milisegundos de la última lectura.
 */
data class ReadingProgress(
    val fileHash: String,
    val fileName: String,
    val paragraphIndex: Int,
    val charOffset: Int,
    val totalParagraphs: Int,
    val lastReadTimestamp: Long = System.currentTimeMillis()
)
