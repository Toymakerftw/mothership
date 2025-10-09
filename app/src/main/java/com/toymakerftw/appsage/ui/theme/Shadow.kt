package com.toymakerftw.appsage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb

fun Modifier.advancedShadow(
    color: Color = Color.Black,
    alpha: Float = 0.08f, // More subtle alpha
    cornersRadius: Dp = 0.dp,
    shadowBlurRadius: Dp = 6.dp, // Smaller blur radius
    offsetY: Dp = 2.dp, // Smaller offset
    offsetX: Dp = 0.dp
) = this.drawBehind {
    val shadowColor = color.copy(alpha = alpha).toArgb()
    val transparent = color.copy(alpha = 0f).toArgb()

    this.drawIntoCanvas {
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = transparent
        
        // Use a more subtle shadow layer
        frameworkPaint.setShadowLayer(
            shadowBlurRadius.toPx(),
            offsetX.toPx(),
            offsetY.toPx(),
            shadowColor
        )
        
        it.drawRoundRect(
            0f,
            0f,
            this.size.width,
            this.size.height,
            cornersRadius.toPx(),
            cornersRadius.toPx(),
            paint
        )
    }
}

// Alternative simpler shadow implementation for better performance
fun Modifier.naturalShadow(
    color: Color = Color.Black,
    alpha: Float = 0.06f,
    elevation: Dp = 4.dp,
    cornersRadius: Dp = 0.dp
) = this.drawBehind {
    val shadowColor = color.copy(alpha = alpha).toArgb()
    val transparent = color.copy(alpha = 0f).toArgb()

    this.drawIntoCanvas {
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = transparent
        
        // More natural shadow parameters
        frameworkPaint.setShadowLayer(
            elevation.toPx() * 1.5f, // Scale blur radius
            0f, // No X offset
            elevation.toPx() * 0.5f, // Subtle Y offset
            shadowColor
        )
        
        it.drawRoundRect(
            0f,
            0f,
            this.size.width,
            this.size.height,
            cornersRadius.toPx(),
            cornersRadius.toPx(),
            paint
        )
    }
}

// For cards that need the exact shadow from your images
fun Modifier.cardShadow() = naturalShadow(
    alpha = 0.05f,
    elevation = 3.dp
)

// For pressed state shadows
fun Modifier.pressedShadow() = naturalShadow(
    alpha = 0.08f,
    elevation = 6.dp
)