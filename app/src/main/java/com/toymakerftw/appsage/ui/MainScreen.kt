@file:OptIn(ExperimentalMaterial3Api::class)

package com.toymakerftw.appsage.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toymakerftw.appsage.MainUiState
import androidx.compose.material3.TextFieldDefaults
import androidx.navigation.NavController
import com.toymakerftw.appsage.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    navController: NavController? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var prompt by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    
    // Animate header on scroll
    val headerScale by animateFloatAsState(
        targetValue = if (scrollState.value > 100) 0.9f else 1f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "header_scale"
    )
    
    val headerAlpha by animateFloatAsState(
        targetValue = if (scrollState.value > 100) 0.8f else 1f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "header_alpha"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header with animation
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { -40 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { -40 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                MainHeader(
                    modifier = Modifier.scale(headerScale),
                    alpha = headerAlpha
                )
            }

            // API Key Status Card - Hidden during generation
            AnimatedVisibility(
                visible = !uiState.isGenerating,
                enter = slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(300, delayMillis = 100, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300, delayMillis = 100)),
                exit = slideOutVertically(
                    targetOffsetY = { -40 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                ApiKeyStatusCard(
                    hasApiKey = !uiState.apiKey.isNullOrBlank(),
                    onConfigure = { navController?.navigate("settings") }
                )
            }

            // User Prompt Card - Only visible during generation
            AnimatedVisibility(
                visible = uiState.isGenerating,
                enter = slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(300, delayMillis = 100, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300, delayMillis = 100)),
                exit = slideOutVertically(
                    targetOffsetY = { -40 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                UserPromptCard(prompt = prompt)
            }

            // Prompt Input Card - Hidden during generation
            AnimatedVisibility(
                visible = !uiState.isGenerating,
                enter = slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(300, delayMillis = 200, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300, delayMillis = 200)),
                exit = slideOutVertically(
                    targetOffsetY = { -40 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                PromptInputCard(
                    prompt = prompt,
                    onPromptChange = { prompt = it }
                )
            }

            // Generate Button - Hidden during generation
            AnimatedVisibility(
                visible = !uiState.isGenerating,
                enter = slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(300, delayMillis = 250, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300, delayMillis = 250)),
                exit = slideOutVertically(
                    targetOffsetY = { -40 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                GenerateButton(
                    prompt = prompt,
                    onGenerate = {
                        if (!uiState.apiKey.isNullOrBlank()) {
                            viewModel.generatePwa(prompt)
                        } else {
                            navController?.navigate("settings")
                        }
                    }
                )
            }

            // Feature Highlights - Hidden during generation, success, or error
            AnimatedVisibility(
                visible = !uiState.isGenerating && !uiState.pwaGenerated && uiState.errorMessage == null,
                enter = slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(300, delayMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300, delayMillis = 300)),
                exit = slideOutVertically(
                    targetOffsetY = { -40 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                FeatureHighlights()
            }

            // Progress Timeline - Only visible during generation
            AnimatedVisibility(
                visible = uiState.isGenerating,
                enter = expandVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            ) {
                ProgressTimeline(
                    currentStep = uiState.generationStep ?: 0
                )
            }

            // Error Message with animation
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { -40 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            ) {
                ErrorCard(errorMessage = uiState.errorMessage ?: "")
            }

            // Success Message with animation
            AnimatedVisibility(
                visible = uiState.pwaGenerated,
                enter = slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { -40 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            ) {
                SuccessCard(onViewApps = { navController?.navigate("app_list") })
            }

            Spacer(modifier = Modifier.height(80.dp)) // Extra space for bottom nav
        }
    }
}

@Composable
private fun FeatureHighlights() {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "What can Appsage do for you?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            FeatureItem(
                icon = Icons.Default.AutoAwesome,
                title = "AI-Powered Generation",
                description = "Create fully functional Progressive Web Apps with just a simple description"
            )
            
            FeatureItem(
                icon = Icons.Default.Code,
                title = "Complete Code Generation",
                description = "Get HTML, CSS, JavaScript, and manifest files ready to deploy"
            )
            
            FeatureItem(
                icon = Icons.Default.Smartphone,
                title = "Mobile-First Design",
                description = "All generated apps are responsive and work perfectly on any device"
            )
            
            FeatureItem(
                icon = Icons.Default.Security,
                title = "Privacy-Focused",
                description = "Your API key and prompts are never stored on our servers"
            )
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun MainHeader(modifier: Modifier = Modifier, alpha: Float = 1f) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🚀", fontSize = 40.sp, modifier = Modifier.scale(scale))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Appsage",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
        )
        
        Text(
            text = "Describe your app, we'll build it",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f * alpha),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun ApiKeyStatusCard(hasApiKey: Boolean, onConfigure: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 3.dp else 1.dp,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "elevation"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = !hasApiKey,
                onClick = onConfigure,
                interactionSource = interactionSource,
                indication = null
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasApiKey) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (hasApiKey) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasApiKey) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (hasApiKey) MaterialTheme.colorScheme.onPrimaryContainer 
                           else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasApiKey) "API Key Configured" else "API Key Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (hasApiKey) MaterialTheme.colorScheme.onSurface 
                           else MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = if (hasApiKey) "Ready to generate apps" 
                           else "Configure API key to start generating",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (hasApiKey) MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
            if (!hasApiKey) {
                FilledTonalButton(
                    onClick = onConfigure,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Configure",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun UserPromptCard(prompt: String) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Creating Your App",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Based on your request",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PromptInputCard(
    prompt: String,
    onPromptChange: (String) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 3.dp else 1.dp,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "elevation"
    )
    
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Create Your App",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Describe what you want to build",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                label = { 
                    Text(
                        "What kind of app do you want?",
                        fontWeight = FontWeight.Medium
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                placeholder = { 
                    Text(
                        "Examples:\n• A weather dashboard with dark mode\n• A todo list with categories\n• A recipe manager with favorites",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}

@Composable
private fun GenerateButton(
    prompt: String,
    onGenerate: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "scale"
    )
    
    FilledTonalButton(
        onClick = onGenerate,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .scale(scale),
        enabled = prompt.isNotBlank(),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Generate App",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProgressTimeline(
    currentStep: Int
) {
    val steps = listOf(
        TimelineStep(Icons.Default.Psychology, "Analyzing prompt", "Understanding your requirements"),
        TimelineStep(Icons.Default.Code, "Generating code", "Creating app structure"),
        TimelineStep(Icons.Default.Brush, "Styling UI", "Applying design elements"),
        TimelineStep(Icons.Default.CheckCircle, "Finalizing", "Preparing your app")
    )

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Generation Progress",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            steps.forEachIndexed { index, step ->
                TimelineItem(
                    icon = step.icon,
                    title = step.title,
                    subtitle = step.subtitle,
                    isCompleted = index < currentStep,
                    isActive = index == currentStep,
                    isLast = index == steps.size - 1
                )
            }
        }
    }
}

data class TimelineStep(
    val icon: ImageVector,
    val title: String,
    val subtitle: String
)

@Composable
private fun TimelineItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isActive: Boolean,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Timeline indicator column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        when {
                            isCompleted -> MaterialTheme.colorScheme.primary
                            isActive -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (isActive) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(
                            if (isCompleted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(20.dp))

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (!isLast) 20.dp else 0.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    isActive -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ErrorCard(errorMessage: String) {
    var isPressed by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 1.dp,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "elevation"
    )
    
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Generation Failed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SuccessCard(onViewApps: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 1.dp,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "elevation"
    )
    
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "App Generated Successfully!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Text(
                text = "Your app is ready to use. View it in your collection to start using it.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            
            Button(
                onClick = onViewApps,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                interactionSource = interactionSource
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "View My Apps",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}