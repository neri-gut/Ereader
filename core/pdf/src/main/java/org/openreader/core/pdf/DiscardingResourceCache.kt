package org.openreader.core.pdf

import com.tom_roush.pdfbox.cos.COSObject
import com.tom_roush.pdfbox.pdmodel.DefaultResourceCache
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.graphics.PDXObject
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColorSpace
import com.tom_roush.pdfbox.pdmodel.graphics.pattern.PDAbstractPattern
import com.tom_roush.pdfbox.pdmodel.graphics.shading.PDShading
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState

internal class DiscardingResourceCache : DefaultResourceCache() {
    override fun put(indirect: COSObject, font: PDFont) = Unit
    override fun put(indirect: COSObject, colorSpace: PDColorSpace) = Unit
    override fun put(indirect: COSObject, extGState: PDExtendedGraphicsState) = Unit
    override fun put(indirect: COSObject, shading: PDShading) = Unit
    override fun put(indirect: COSObject, pattern: PDAbstractPattern) = Unit
    override fun put(indirect: COSObject, xobject: PDXObject) = Unit
}
