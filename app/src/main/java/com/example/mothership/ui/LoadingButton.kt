package com.example.mothership.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun LoadingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    shape: Shape = RoundedCornerShape(16.dp),
    normalText: String = "Launch PWA Generation"
) {
    var isPressed by remember { mutableStateOf(false) }
    var currentMessageIndex by remember { mutableStateOf(0) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = tween(100),
        label = "button_scale"
    )
    
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 4.dp.value else 8.dp.value,
        animationSpec = tween(100),
        label = "button_elevation"
    )

    // Witty loading messages that cycle every 2.5 seconds
    val loadingMessages = listOf(
        "🧠 AI neurons firing...",
        "⚡ Coding at light speed...",
        "🎨 Designing your masterpiece...",
        "🔧 Assembling components...",
        "✨ Adding some magic...",
        "🚀 Preparing for launch...",
        "💎 Polishing the details...",
        "🎯 Almost there...",
        "🌟 Final touches...",
        "🔮 Manifesting your vision...",
        "⚙️ Fine-tuning algorithms...",
        "🎪 Creating digital circus..."
    )

    // Cycle through messages when loading
    LaunchedEffect(isLoading) {
        if (isLoading) {
            while (true) {
                delay(2500)
                currentMessageIndex = (currentMessageIndex + 1) % loadingMessages.size
            }
        } else {
            currentMessageIndex = 0
        }
    }

    // Infinite progress animation
    val infiniteTransition = rememberInfiniteTransition(label = "progress")
    val progressAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = elevation.dp,
                shape = shape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
    ) {
        Button(
            onClick = onClick,
            enabled = enabled && !isLoading,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isLoading) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                else 
                    MaterialTheme.colorScheme.primary,
                contentColor = if (isLoading)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            ),
            contentPadding = PaddingValues(0.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                disabledElevation = 0.dp
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                // Loading state with integrated progress
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Background progress bar
                    LinearProgressIndicator(
                        progress = progressAnimation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(shape),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                    )
                    
                    // Content overlay
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🚀",
                            fontSize = 20.sp,
                            modifier = Modifier.scale(1.2f)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = loadingMessages[currentMessageIndex],
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                // Normal state
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RocketLaunch,
                        contentDescription = "Generate",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = normalText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}