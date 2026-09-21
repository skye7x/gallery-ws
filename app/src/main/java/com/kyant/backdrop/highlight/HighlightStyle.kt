package com.kyant.backdrop.highlight

import androidx.annotation.FloatRange
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceAtMost
import com.kyant.backdrop.RuntimeShader
import com.kyant.backdrop.RuntimeShaderCache
import com.kyant.backdrop.internal.AmbientHighlightShaderString
import com.kyant.backdrop.internal.DefaultHighlightShaderString
import com.kyant.backdrop.isRuntimeShaderSupported
import kotlin.math.PI

@Immutable
interface HighlightStyle {
    val color: Color
    val blendMode: BlendMode

    fun DrawScope.createShader(
        shape: Shape,
        runtimeShaderCache: RuntimeShaderCache
    ): RuntimeShader?

    @Immutable
    data class Plain(
        override val color: Color = Color.White.copy(alpha = 0.38f),
        override val blendMode: BlendMode = BlendMode.Plus
    ) : HighlightStyle {
        override fun DrawScope.createShader(
            shape: Shape,
            runtimeShaderCache: RuntimeShaderCache
        ): RuntimeShader? = null
    }

    @Immutable
    data class Default(
        override val color: Color = Color.White.copy(alpha = 0.5f),
        override val blendMode: BlendMode = BlendMode.Plus,
        val angle: Float = 45f,
        @param:FloatRange(from = 0.0) val falloff: Float = 1f
    ) : HighlightStyle {
        override fun DrawScope.createShader(
            shape: Shape,
            runtimeShaderCache: RuntimeShaderCache
        ): RuntimeShader? {
            return if (isRuntimeShaderSupported()) {
                runtimeShaderCache.obtainRuntimeShader(
                    "Default",
                    DefaultHighlightShaderString
                ).apply {
                    setFloatUniform("size", size.width, size.height)
                    setFloatUniform("cornerRadii", getCornerRadii(shape))
                    setColorUniform("color", color.copy(alpha = 1f))
                    setFloatUniform("angle", angle * (PI / 180f).toFloat())
                    setFloatUniform("falloff", falloff)
                }
            } else {
                null
            }
        }
    }

    @Immutable
    data class Ambient(
        @param:FloatRange(from = 0.0, to = 1.0) val intensity: Float = 0.38f
    ) : HighlightStyle {
        override val color: Color = Color.White.copy(alpha = intensity)
        override val blendMode: BlendMode = DrawScope.DefaultBlendMode

        override fun DrawScope.createShader(
            shape: Shape,
            runtimeShaderCache: RuntimeShaderCache
        ): RuntimeShader? {
            return if (isRuntimeShaderSupported()) {
                runtimeShaderCache.obtainRuntimeShader(
                    "Ambient",
                    AmbientHighlightShaderString
                ).apply {
                    setFloatUniform("size", size.width, size.height)
                    setFloatUniform("cornerRadii", getCornerRadii(shape))
                    setFloatUniform("angle", 45f * (PI / 180f).toFloat())
                    setFloatUniform("falloff", 1f)
                }
            } else {
                null
            }
        }
    }

    companion object {
        @Stable
        val Default: Default = Default()
        @Stable
        val Ambient: Ambient = Ambient()
        @Stable
        val Plain: Plain = Plain()
    }
}

private fun DrawScope.getCornerRadii(shape: Shape): FloatArray {
    val sz = size
    val maxRadius = sz.minDimension / 2f
    val s = shape as? CornerBasedShape ?: return FloatArray(4) { maxRadius }
    val isLtr = layoutDirection == LayoutDirection.Ltr
    val topLeft =
        if (isLtr) s.topStart.toPx(sz, this)
        else s.topEnd.toPx(sz, this)
    val topRight =
        if (isLtr) s.topEnd.toPx(sz, this)
        else s.topStart.toPx(sz, this)
    val bottomRight =
        if (isLtr) s.bottomEnd.toPx(sz, this)
        else s.bottomStart.toPx(sz, this)
    val bottomLeft =
        if (isLtr) s.bottomStart.toPx(sz, this)
        else s.bottomEnd.toPx(sz, this)
    return floatArrayOf(
        topLeft.fastCoerceAtMost(maxRadius),
        topRight.fastCoerceAtMost(maxRadius),
        bottomRight.fastCoerceAtMost(maxRadius),
        bottomLeft.fastCoerceAtMost(maxRadius)
    )
}
