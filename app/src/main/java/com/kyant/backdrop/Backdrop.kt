package com.kyant.backdrop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density

interface Backdrop {
    val isCoordinatesDependent: Boolean

    fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)? = null
    )
}

@Immutable
object EmptyBackdrop : Backdrop {
    override val isCoordinatesDependent: Boolean = false
    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {}
}

fun emptyBackdrop(): Backdrop = EmptyBackdrop

@Composable
fun rememberBackdrop(
    backdrop: Backdrop,
    onDraw: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit
): Backdrop {
    return remember(backdrop, onDraw) {
        CustomBackdrop(backdrop, onDraw)
    }
}

@Immutable
private class CustomBackdrop(
    val backdrop: Backdrop,
    val onDraw: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit
) : Backdrop {
    override val isCoordinatesDependent: Boolean = backdrop.isCoordinatesDependent

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        onDraw { with(backdrop) { drawBackdrop(density, coordinates, layerBlock) } }
    }
}
