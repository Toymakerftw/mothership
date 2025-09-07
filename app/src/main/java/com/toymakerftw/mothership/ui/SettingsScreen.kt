package com.toymakerftw.mothership.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.toymakerftw.mothership.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel) {
    val apiKey by settingsViewModel.apiKey.collectAsState()
    var newApiKey by remember { mutableStateOf(apiKey ?: "") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // Get screen configuration for responsive sizing
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isTablet = screenWidth > 600
    
    // Calculate responsive sizes
    val padding = if (isTablet) 40.dp else 20.dp
    val headerPadding = if (isTablet) 48.dp else 32.dp
    val headerIconSize = if (isTablet) 80.dp else 60.dp
    val headerIconTextSize = if (isTablet) 32.sp else 24.sp
    val cardPadding = if (isTablet) 36.dp else 24.dp
    val cardCornerRadius = if (isTablet) 32.dp else 20.dp
    val buttonHeight = if (isTablet) 64.dp else 56.dp
    val buttonCornerRadius = if (isTablet) 20.dp else 16.dp
    val iconSize = if (isTablet) 32.dp else 24.dp
    val stepIconSize = if (isTablet) 48.dp else 40.dp
    val stepIconTextSize = if (isTablet) 20.sp else 16.sp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                    )
                )
            )
            .verticalScroll(scrollState)
            .padding(padding)
    ) {
        // Header with rocket
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = headerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(headerIconSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🚀", fontSize = headerIconTextSize)
            }
            
            Spacer(modifier = Modifier.height(if (isTablet) 16.dp else 12.dp))
            
            Text(
                text = "Settings",
                style = if (isTablet) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Configure your Mothership experience",
                style = if (isTablet) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = if (isTablet) 8.dp else 4.dp)
            )
        }

        // API Configuration Card
        SettingsCard(
            icon = Icons.Outlined.Key,
            title = "API Configuration",
            subtitle = "Secure API key management",
            cardPadding = cardPadding,
            cardCornerRadius = cardCornerRadius,
            iconSize = iconSize
        ) {
            Text(
                text = "Your OpenRouter API key enables PWA generation. It's stored securely on your device and only sent to OpenRouter's servers.",
                style = if (isTablet) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = if (isTablet) 32.dp else 24.dp),
                lineHeight = if (isTablet) 24.sp else 20.sp
            )

            OutlinedTextField(
                value = newApiKey,
                onValueChange = { newApiKey = it },
                label = { 
                    Text(
                        "OpenRouter API Key",
                        fontWeight = FontWeight.Medium,
                        fontSize = if (isTablet) 18.sp else 16.sp
                    ) 
                },
                placeholder = { 
                    Text(
                        "Enter your API key here",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = if (isTablet) 16.sp else 14.sp
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isTablet) 28.dp else 20.dp),
                shape = RoundedCornerShape(if (isTablet) 20.dp else 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isPasswordVisible) "Hide API key" else "Show API key",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(if (isTablet) 28.dp else 20.dp)
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (isTablet) 28.dp else 20.dp)
                    )
                }
            )

            AnimatedButton(
                onClick = {
                    settingsViewModel.saveApiKey(newApiKey)
                    Toast.makeText(context, "API Key Saved Successfully! 🚀", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
                shape = RoundedCornerShape(buttonCornerRadius)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Save",
                    modifier = Modifier.size(if (isTablet) 28.dp else 20.dp)
                )
                Spacer(modifier = Modifier.width(if (isTablet) 12.dp else 8.dp))
                Text(
                    text = "Save API Key",
                    style = if (isTablet) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isTablet) 20.sp else 16.sp
                )
            }
            
            if (newApiKey.isEmpty()) {
                Text(
                    text = "Don't have an API key? The app includes a free demo mode with 5 daily uses. Add your own key for unlimited access.",
                    style = if (isTablet) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = if (isTablet) 24.dp else 16.dp),
                    lineHeight = if (isTablet) 22.sp else 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(if (isTablet) 32.dp else 20.dp))

        // Demo Mode Card
        SettingsCard(
            icon = Icons.Filled.Info,
            title = "Demo Mode",
            subtitle = "Free trial with limited usage",
            cardPadding = cardPadding,
            cardCornerRadius = cardCornerRadius,
            iconSize = iconSize
        ) {
            Text(
                text = "Try Mothership with our free demo mode that includes 5 API calls per day. " +
                        "No registration required - just start creating!\n\n" +
                        "To unlock unlimited usage, add your own OpenRouter API key above. " +
                        "It's free to get started at openrouter.ai.",
                style = if (isTablet) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = if (isTablet) 24.dp else 16.dp),
                lineHeight = if (isTablet) 24.sp else 20.sp
            )
        }

        Spacer(modifier = Modifier.height(if (isTablet) 32.dp else 20.dp))

        // How It Works Card
        SettingsCard(
            icon = Icons.Filled.Lightbulb,
            title = "How It Works",
            subtitle = "Simple steps to create amazing PWAs",
            cardPadding = cardPadding,
            cardCornerRadius = cardCornerRadius,
            iconSize = iconSize
        ) {
            val steps = listOf(
                "Create" to "Describe your app idea using natural language",
                "Generate" to "AI creates complete HTML, CSS, and JavaScript",
                "Deploy" to "View, install, or share your new PWA instantly"
            )

            steps.forEachIndexed { index, (title, description) ->
                StepItem(
                    stepNumber = index + 1,
                    title = title,
                    description = description,
                    isLast = index == steps.size - 1,
                    stepIconSize = stepIconSize,
                    stepIconTextSize = stepIconTextSize,
                    isTablet = isTablet
                )
            }
        }

        Spacer(modifier = Modifier.height(if (isTablet) 120.dp else 100.dp)) // Space for bottom navigation
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    cardPadding: androidx.compose.ui.unit.Dp = 24.dp,
    cardCornerRadius: androidx.compose.ui.unit.Dp = 20.dp,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(iconSize * 2)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(iconSize)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            content()
        }
    }
}

@Composable
private fun StepItem(
    stepNumber: Int,
    title: String,
    description: String,
    isLast: Boolean,
    stepIconSize: androidx.compose.ui.unit.Dp = 40.dp,
    stepIconTextSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    isTablet: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLast) 0.dp else if (isTablet) 28.dp else 20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(stepIconSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber.toString(),
                style = if (isTablet) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = stepIconTextSize
            )
        }
        
        Spacer(modifier = Modifier.width(if (isTablet) 20.dp else 16.dp))
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = if (isTablet) 4.dp else 2.dp)
        ) {
            Text(
                text = title,
                style = if (isTablet) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = if (isTablet) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = if (isTablet) 8.dp else 4.dp),
                lineHeight = if (isTablet) 22.sp else 18.sp
            )
        }
    }
}