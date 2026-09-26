package org.openreader.feature.reader

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val Int.rem: Dp get() = (this * 16).dp
