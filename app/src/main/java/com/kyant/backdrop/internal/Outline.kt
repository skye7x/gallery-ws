package com.kyant.backdrop.internal

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path

internal fun Canvas.clipOutline(outline: Outline, path: Path?) {
    when (outline) {
        is Outline.Rectangle -> clipRect(outline.rect)
        is Outline.Rounded -> {
            val p = path ?: Path()
            p.rewind()
            p.addRoundRect(outline.roundRect)
            clipPath(p)
        }
        is Outline.Generic -> clipPath(outline.path)
    }
}
