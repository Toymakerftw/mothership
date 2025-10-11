// ReworkScreen.kt
package com.toymakerftw.appsage.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.toymakerftw.appsage.PwaViewerActivity
import com.toymakerftw.appsage.ReworkViewModel
import com.toymakerftw.appsage.service.PwaManager

import com.toymakerftw.appsage.versioncontrol.VersionInfo
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.toymakerftw.appsage.ui.theme.CardConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReworkScreen(
    uuid: String,
    viewModel: ReworkViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val pwaManager = remember { PwaManager(context) }
    var reworkPrompt by remember { mutableStateOf("") }
    var isPreviewing by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val pwaDir = remember { File(context.getExternalFilesDir(null), uuid) }
    val isAppReady = remember { File(pwaDir, "index.html").exists() }
    
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

        // Top Bar with Back Button
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(headerScale),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Rework App",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = headerAlpha)
                )
            }
        }

        // UUID Card - Hidden during rework
        AnimatedVisibility(
            visible = !uiState.isReworking,
            enter = slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = tween(300, delayMillis = 100, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300, delayMillis = 100)),
            exit = slideOutVertically(
                targetOffsetY = { -40 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = CardConstants.mediumShape, // Consistent radius
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "App ID",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = uuid,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // User Prompt Card - Only visible during rework
        AnimatedVisibility(
            visible = uiState.isReworking,
            enter = slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = tween(300, delayMillis = 100, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300, delayMillis = 100)),
            exit = slideOutVertically(
                targetOffsetY = { -40 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            ReworkPromptCard(prompt = reworkPrompt)
        }

        // Rework Prompt - Hidden during rework
        AnimatedVisibility(
            visible = !uiState.isReworking,
            enter = slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = tween(300, delayMillis = 200, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300, delayMillis = 200)),
            exit = slideOutVertically(
                targetOffsetY = { -40 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = CardConstants.largeShape, // Consistent radius
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Modification Instructions",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Describe the changes you want",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = reworkPrompt,
                        onValueChange = { reworkPrompt = it },
                        label = {
                            Text(
                                "What changes do you want?",
                                fontWeight = FontWeight.Medium
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        placeholder = {
                            Text(
                                "Examples:\n• Add dark mode toggle\n• Change header color\n• Add search bar",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        shape = CardConstants.mediumShape, // Consistent radius
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        }

        // Action Buttons - Hidden during rework
        AnimatedVisibility(
            visible = !uiState.isReworking,
            enter = slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = tween(300, delayMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300, delayMillis = 300)),
            exit = slideOutVertically(
                targetOffsetY = { -40 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { 
                        viewModel.reworkPwa(uuid, reworkPrompt)
                        isPreviewing = false
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    enabled = reworkPrompt.isNotBlank(),
                    shape = CardConstants.mediumShape, // Consistent button radius
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Apply Changes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }


                Button(
                    onClick = { 
                        launchPreview(uuid, context, pwaManager)
                        isPreviewing = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    enabled = isAppReady,
                    shape = CardConstants.mediumShape, // Consistent button radius
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.pwaReworked) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.secondary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    val buttonText = if (uiState.pwaReworked) "View Updated App" else "Preview App"
                    
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = buttonText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Progress Timeline - Only visible during rework
        AnimatedVisibility(
            visible = uiState.isReworking,
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
            ReworkProgressTimeline(
                currentStep = uiState.generationStep ?: 1
            )
        }

        // Status Messages
        AnimatedVisibility(
            visible = uiState.errorMessage != null,
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
            uiState.errorMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = CardConstants.smallShape, // Consistent radius
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // Success Message
        AnimatedVisibility(
            visible = uiState.pwaReworked && !uiState.isReworking,
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
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = CardConstants.smallShape, // Consistent radius
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App successfully reworked! 🎉",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap 'View Updated App' to see your changes",
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Revert Success Message
        AnimatedVisibility(
            visible = uiState.pwaReverted,
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
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = CardConstants.smallShape, // Consistent radius
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App reverted to previous version! 🔄",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Changes have been restored successfully",
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        // Version History Card - Hidden during rework
        AnimatedVisibility(
            visible = !uiState.isReworking,
            enter = slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = tween(300, delayMillis = 400, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300, delayMillis = 400)),
            exit = slideOutVertically(
                targetOffsetY = { -40 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            VersionHistoryCard(uuid = uuid, viewModel = viewModel)
        }
    }
}

@Composable
private fun ReworkPromptCard(prompt: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = CardConstants.largeShape, // Consistent radius
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Modifying Your App",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Based on your modifications",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardConstants.mediumShape, // Consistent radius
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
private fun ReworkProgressTimeline(
    currentStep: Int
) {
    val steps = listOf(
        TimelineStep(Icons.Default.Backup, "Creating backup", "Saving current version"),
        TimelineStep(Icons.Default.Psychology, "Analyzing changes", "Understanding your modifications"),
        TimelineStep(Icons.Default.Code, "Updating code", "Applying your changes"),
        TimelineStep(Icons.Default.Brush, "Refreshing UI", "Updating the interface"),
        TimelineStep(Icons.Default.CheckCircle, "Finalizing", "Preparing updated app")
    )

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = CardConstants.extraLargeShape, // Consistent radius
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Rework Progress",
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
                    isCompleted = index < currentStep - 1, // Fixed: Adjusted for 1-based indexing
                    isActive = index == currentStep - 1, // Fixed: Adjusted for 1-based indexing
                    isLast = index == steps.size - 1
                )
            }
        }
    }
}

@Composable
private fun TimelineItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isActive: Boolean,
    isLast: Boolean
) {
    // Add animation for the active step
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
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
                    // Add pulsing animation to the active step
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).scale(scale),
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

// Helper function to launch preview
private fun launchPreview(uuid: String, context: android.content.Context, pwaManager: PwaManager) {
    val pwaDir = File(context.getExternalFilesDir(null), uuid)
    val indexFile = File(pwaDir, "index.html")
    if (!indexFile.exists()) {
        Toast.makeText(context, "App not ready yet", Toast.LENGTH_SHORT).show()
        return
    }

    val port = pwaManager.generateUniquePort(uuid)
    var pwaName = "Preview App"
    try {
        val manifestFile = File(pwaDir, "manifest.json")
        if (manifestFile.exists()) {
            val manifestJson = JSONObject(manifestFile.readText())
            val shortName = manifestJson.optString("short_name", "")
            val name = manifestJson.optString("name", "")
            val betterName = if (shortName.isNotEmpty()) shortName else name
            if (betterName.isNotEmpty()) pwaName = betterName
        }
    } catch (_: Exception) {}

    if (pwaManager.startPwaServer(uuid, port)) {
        val intent = Intent(context, PwaViewerActivity::class.java).apply {
            putExtra("pwaUrl", "http://127.0.0.1:$port/index.html")
            putExtra("pwaName", pwaName)
            putExtra("pwaId", uuid)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "Failed to start preview server", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun VersionHistoryCard(uuid: String, viewModel: ReworkViewModel) {
    val versionHistory by remember { mutableStateOf(viewModel.getVersionHistory(uuid)) }
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = CardConstants.largeShape, // Consistent radius
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Version History",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${versionHistory.versions.size} versions saved",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (versionHistory.versions.isEmpty()) {
                        Text(
                            text = "No version history available yet. Rework your app to create backups.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp), // Constrain height to avoid infinite constraints
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(versionHistory.versions) { version ->
                                VersionItem(version = version, uuid = uuid, viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionItem(version: VersionInfo, uuid: String, viewModel: ReworkViewModel) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(version.timestamp))
    val timeStr = timeFormat.format(Date(version.timestamp))
    
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = CardConstants.mediumShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header row with icon and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Revert button
                Button(
                    onClick = {
                        if (viewModel.revertToVersion(uuid, version.versionId)) {
                            Toast.makeText(context, "Version restored successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to restore version", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = CardConstants.smallShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Revert",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Commit message if available
            if (version.commitMessage.isNotEmpty() && version.commitMessage != "Auto-backup before rework") {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardConstants.smallShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = version.commitMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}