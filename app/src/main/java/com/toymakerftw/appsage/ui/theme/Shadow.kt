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

/**
 * Material 3 Expressive Shadow Implementation
 * 
 * This custom shadow implementation aligns with Material 3's expressive style,
 * using more pronounced shadows with appropriate depth and contrast for the
 * expressive design approach.
 */
fun Modifier.expressiveShadow(
    color: Color = Color.Black,
    alpha: Float = 0.08f,
    cornersRadius: Dp = 20.dp, // Default to expressive rounded corners
    shadowBlurRadius: Dp = 8.dp, // More pronounced for expressive style
    offsetY: Dp = 4.dp,
    offsetX: Dp = 0.dp
) = this.drawBehind {
    val shadowColor = color.copy(alpha = alpha).toArgb()
    val transparent = color.copy(alpha = 0f).toArgb()

    this.drawIntoCanvas {
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = transparent
        
        // Material 3 expressive shadow parameters
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

// Alternative implementation for different expressive shadow levels
fun Modifier.expressiveElevation(
    color: Color = Color.Black,
    alpha: Float = 0.06f,
    elevation: Dp = 4.dp,
    cornersRadius: Dp = 20.dp
) = this.drawBehind {
    val shadowColor = color.copy(alpha = alpha).toArgb()
    val transparent = color.copy(alpha = 0f).toArgb()

    this.drawIntoCanvas {
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = transparent
        
        // More expressive shadow parameters for Material 3
        frameworkPaint.setShadowLayer(
            elevation.toPx() * 1.5f,
            0f,
            elevation.toPx() * 0.5f,
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

// Keep the old function names for compatibility with existing code
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

// For expressive cards with pronounced shadows
fun Modifier.expressiveCardShadow() = expressiveShadow(
    alpha = 0.08f,
    shadowBlurRadius = 4.dp,
    cornersRadius = 24.dp
)

// For pressed state expressive shadows
fun Modifier.expressivePressedShadow() = expressiveShadow(
    alpha = 0.12f,
    shadowBlurRadius = 8.dp,
    cornersRadius = 24.dp
)