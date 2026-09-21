package com.kyant.backdrop.effects

import androidx.annotation.FloatRange
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.internal.RoundedRectRefractionShaderString
import com.kyant.backdrop.internal.RoundedRectRefractionWithDispersionShaderString
import com.kyant.backdrop.internal.RuntimeShaderEffect
import com.kyant.backdrop.isRuntimeShaderSupported

fun BackdropEffectScope.lens(
    @FloatRange(from = 0.0) refractionHeight: Float,
    @FloatRange(from = 0.0) refractionAmount: Float,
    depthEffect: Boolean = false,
    chromaticAberration: Boolean = false
) {
    if (!isRuntimeShaderSupported()) return
    val sz = size
    if (!sz.isSpecified || sz.width <= 0f || sz.height <= 0f) return
    if (refractionHeight <= 0f || refractionAmount <= 0f) return
    if (padding > 0f) {
        padding = (padding - refractionHeight).fastCoerceAtLeast(0f)
    }
    val cornerRadii = cornerRadii
    val effect =
        if (cornerRadii != null) {
            val shader =
                if (!chromaticAberration) {
                    obtainRuntimeShader(
                        "Refraction",
                        RoundedRectRefractionShaderString
                    )
                } else {
                    obtainRuntimeShader(
                        "RefractionWithDispersion",
                        RoundedRectRefractionWithDispersionShaderString
                    )
                }
            shader.apply {
                setFloatUniform("size", sz.width, sz.height)
                setFloatUniform("offset", -padding, -padding)
                setFloatUniform("cornerRadii", cornerRadii)
                setFloatUniform("refractionHeight", refractionHeight)
                setFloatUniform("refractionAmount", -refractionAmount)
                setFloatUniform("depthEffect", if (depthEffect) 1f else 0f)
                if (chromaticAberration) {
                    setFloatUniform("chromaticAberration", 1f)
                }
            }
            RuntimeShaderEffect(shader, "content")
        } else {
            return
        }
    if (effect != null) {
        effect(effect)
    }
}

private val BackdropEffectScope.cornerRadii: FloatArray?
    get() {
        val sz = size
        if (!sz.isSpecified || sz.width <= 0f || sz.height <= 0f) return null
        return when (val s = shape) {
            is AbsoluteRoundedCornerShape -> {
                val maxRadius = sz.minDimension / 2f
                val topLeft = s.topStart.toPx(sz, this)
                val topRight = s.topEnd.toPx(sz, this)
                val bottomRight = s.bottomEnd.toPx(sz, this)
                val bottomLeft = s.bottomStart.toPx(sz, this)
                floatArrayOf(
                    topLeft.fastCoerceAtMost(maxRadius),
                    topRight.fastCoerceAtMost(maxRadius),
                    bottomRight.fastCoerceAtMost(maxRadius),
                    bottomLeft.fastCoerceAtMost(maxRadius)
                )
            }
            is CornerBasedShape -> {
                val maxRadius = sz.minDimension / 2f
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
                floatArrayOf(
                    topLeft.fastCoerceAtMost(maxRadius),
                    topRight.fastCoerceAtMost(maxRadius),
                    bottomRight.fastCoerceAtMost(maxRadius),
                    bottomLeft.fastCoerceAtMost(maxRadius)
                )
            }
            else -> null
        }
    }
