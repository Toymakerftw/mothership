package com.toymakerftw.appsage.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.toymakerftw.appsage.ui.theme.advancedShadow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toymakerftw.appsage.service.PwaManager
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.toymakerftw.appsage.PwaInstaller
import java.io.File
import kotlinx.coroutines.delay

@Composable
fun PwaListScreen(context: Context) {
    val pwaManager = remember { PwaManager(context) }
    var pwas by remember { mutableStateOf(emptyList<PwaManager.PwaInfo>()) }
    var isLoading by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val firstVisibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    
    // Animate items when they come into view
    val animatedItems = remember { mutableStateListOf<Boolean>() }
    
    LaunchedEffect(Unit) {
        pwas = pwaManager.getGeneratedPwas()
        isLoading = false
        // Initialize animation states
        repeat(pwas.size) { animatedItems.add(false) }
    }
    
    // Trigger animations when items become visible
    LaunchedEffect(firstVisibleItemIndex) {
        val visibleRange = firstVisibleItemIndex..(firstVisibleItemIndex + 3)
        visibleRange.forEach { index ->
            if (index < animatedItems.size && !animatedItems[index]) {
                delay(index * 50L) // Stagger the animations
                animatedItems[index] = true
            }
        }
    }
    
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
            .padding(16.dp)
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
            Text(
                text = "Generated PWAs",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        if (isLoading) {
            // Loading state with animation
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
            }
        } else if (pwas.isEmpty()) {
            // Empty state with animation
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { -40 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No PWAs generated yet",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            // PWA list with staggered animations
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(pwas) { index, pwa ->
                    val isVisible = remember { mutableStateOf(index < 3) }
                    
                    LaunchedEffect(firstVisibleItemIndex) {
                        if (index >= firstVisibleItemIndex - 1 && index <= firstVisibleItemIndex + 3) {
                            isVisible.value = true
                        }
                    }
                    
                    AnimatedVisibility(
                        visible = isVisible.value,
                        enter = slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(
                                durationMillis = 300,
                                delayMillis = index * 50,
                                easing = FastOutSlowInEasing
                            )
                        ) + fadeIn(
                            animationSpec = tween(
                                durationMillis = 300,
                                delayMillis = index * 50,
                                easing = FastOutSlowInEasing
                            )
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { -40 },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(300))
                    ) {
                        PwaItemCard(
                            pwa = pwa,
                            onDelete = {
                                pwaManager.deletePwa(pwa.uuid)
                                pwas = pwaManager.getGeneratedPwas()
                            },
                            onInstall = {
                                val installer = PwaInstaller(context)
                                installer.install(pwa.uuid)
                            },
                            onUninstall = {
                                val installer = PwaInstaller(context)
                                installer.uninstall(pwa.uuid)
                            },
                            onLaunch = {
                                // Generate a unique port for this PWA to avoid conflicts
                                val port = pwaManager.generateUniquePort(pwa.uuid)
                                
                                // Start the PWA server
                                if (pwaManager.startPwaServer(pwa.uuid, port)) {
                                    // Try to get a better name from manifest.json
                                    var pwaName = pwa.name
                                    try {
                                        val pwaDir = File(context.getExternalFilesDir(null), pwa.uuid)
                                        val manifestFile = File(pwaDir, "manifest.json")
                                        
                                        if (manifestFile.exists()) {
                                            val manifestContent = manifestFile.readText()
                                            val manifestJson = org.json.JSONObject(manifestContent)
                                            val shortName = manifestJson.optString("short_name", "")
                                            val manifestName = manifestJson.optString("name", "")
                                            
                                            // Prefer short_name, fallback to name from manifest
                                            val betterName = if (shortName.isNotEmpty()) shortName else manifestName
                                            if (betterName.isNotEmpty()) {
                                                pwaName = betterName
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // If manifest parsing fails, keep the original name
                                    }
                                    
                                    // Launch the PWA viewer
                                    val intent = Intent(context, Class.forName("com.toymakerftw.appsage.PwaViewerActivity")).apply {
                                        putExtra("pwaUrl", "http://127.0.0.1:$port/index.html")
                                        putExtra("pwaName", pwaName)
                                        putExtra("pwaId", pwa.uuid)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    ContextCompat.startActivity(context, intent, null)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PwaItemCard(
    pwa: PwaManager.PwaInfo,
    onDelete: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onLaunch: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable {
                isPressed = true
                onLaunch()
            }
            .advancedShadow(
                cornersRadius = 16.dp,
                shadowBlurRadius = if (isPressed) 8.dp else 4.dp
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = pwa.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (pwa.description.isNotEmpty()) {
                        Text(
                            text = pwa.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "UUID: ${pwa.uuid}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Action buttons with animations
                Row {
                    ActionButton(
                        icon = Icons.Default.PlayArrow,
                        contentDescription = "Launch",
                        onClick = onLaunch
                    )
                    ActionButton(
                        icon = Icons.Default.AddCircle,
                        contentDescription = "Install",
                        onClick = onInstall
                    )
                    ActionButton(
                        icon = Icons.Default.Delete,
                        contentDescription = "Uninstall",
                        onClick = onUninstall
                    )
                    ActionButton(
                        icon = Icons.Default.Delete,
                        contentDescription = "Delete",
                        onClick = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 1f,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "scale"
    )
    
    IconButton(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = Modifier.scale(scale)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription
        )
    }
}