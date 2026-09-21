package com.bzygordev.gallery.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.emptyBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    backdrop: Backdrop = emptyBackdrop(),
    shape: Shape = RoundedCornerShape(20.dp),
    blurRadius: Dp = 16.dp,
    containerColor: Color? = null,
    content: @Composable () -> Unit
) {
    val isLight = !isSystemInDarkTheme()
    val defaultContainer =
        if (isLight) Color(0xFFF7F7F8).copy(alpha = 0.55f)
        else Color(0xFF1C1C1E).copy(alpha = 0.60f)

    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(blurRadius.toPx())
                    lens(12f.dp.toPx(), 16f.dp.toPx())
                },
                highlight = { Highlight.Default.copy(alpha = 0.4f) },
                shadow = { Shadow(radius = 12.dp, color = Color.Black.copy(alpha = 0.08f)) },
                onDrawSurface = { drawRect(containerColor ?: defaultContainer) }
            )
            .clip(shape)
    ) {
        content()
    }
}

@Composable
fun LiquidSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop = emptyBackdrop()
) {
    val isLight = !isSystemInDarkTheme()
    val containerBg =
        if (isLight) Color(0xFFE5E5EA).copy(alpha = 0.5f)
        else Color(0xFF2C2C2E).copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .height(36.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(8f.dp.toPx())
                },
                highlight = { Highlight.Plain.copy(alpha = 0.3f) },
                onDrawSurface = { drawRect(containerBg) }
            )
            .clip(Capsule())
            .padding(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, text ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(Capsule())
                        .background(
                            if (isSelected) {
                                if (isLight) Color.White.copy(alpha = 0.85f)
                                else Color(0xFF636366).copy(alpha = 0.85f)
                            } else Color.Transparent
                        )
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onItemSelected(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        color = if (isSelected) {
                            if (isLight) Color.Black else Color.White
                        } else {
                            if (isLight) Color(0xFF3C3C43).copy(alpha = 0.7f)
                            else Color(0xFFEBEBF5).copy(alpha = 0.6f)
                        },
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
