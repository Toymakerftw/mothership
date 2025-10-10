@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.toymakerftw.appsage.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.navigation.NavController
import com.toymakerftw.appsage.MainViewModel
import com.toymakerftw.appsage.PwaInstaller
import com.toymakerftw.appsage.PwaViewerActivity
import com.toymakerftw.appsage.service.PwaManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.lingala.zip4j.ZipFile
import java.io.File
import org.json.JSONObject

@Composable
fun AppListScreen(navController: NavController, viewModel: MainViewModel) {
    val context = LocalContext.current
    val pwaManager = remember { PwaManager(context) }
    var pwas by remember { mutableStateOf(emptyList<PwaManager.PwaInfo>()) }
    var isLoading by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    
    LaunchedEffect(Unit) {
        pwas = pwaManager.getGeneratedPwas()
        isLoading = false
    }

    // Continuously monitor for deletion events
    LaunchedEffect(Unit) {
        viewModel.uiState.collect { state ->
            if (state.pwaDeleted) {
                pwas = pwaManager.getGeneratedPwas()
                viewModel.clearPwaDeleted()
            }
        }
    }

    // Animate header on scroll
    val headerScale by animateFloatAsState(
        targetValue = if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 100) 0.9f else 1f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "header_scale"
    )
    
    val headerAlpha by animateFloatAsState(
        targetValue = if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 100) 0.8f else 1f,
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
                .padding(20.dp)
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(headerScale)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📱", fontSize = 24.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Your App Collection",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = headerAlpha)
                    )

                    Text(
                        text = "${pwas.size} ${if (pwas.size == 1) "app" else "apps"} generated",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f * headerAlpha),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

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
                if (isLoading) {
                    LoadingState()
                } else if (pwas.isEmpty()) {
                    EmptyState(navController = navController)
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        itemsIndexed(pwas) { _, pwa ->
                            AppCard(
                                pwa = pwa,
                                navController = navController,
                                context = context,
                                onDelete = {
                                    viewModel.deletePwa(pwa.uuid)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    
    Card(
        modifier = Modifier
            .fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Loading Your Apps",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "We're gathering your Apps together",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(navController: NavController) {
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
    
    Card(
        modifier = Modifier
            .fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🌟",
                        fontSize = 48.sp,
                        modifier = Modifier.scale(scale)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Ready for Launch!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "You haven't created any Apps yet. Start building your first app and watch the magic happen!",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 12.dp, bottom = 32.dp),
                    lineHeight = 22.sp
                )

                Button(
                    onClick = { navController.navigate("main") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = "🚀", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Create Your First App",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AppCard(
    pwa: PwaManager.PwaInfo,
    navController: NavController,
    context: Context,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val pwaDir = remember { File(context.getExternalFilesDir(null), pwa.uuid) }
    val hasIndexFile = remember(pwaDir) { File(pwaDir, "index.html").exists() }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "expand_rotation",
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    )
    var isInstalled by remember(pwa.uuid) {
        mutableStateOf(isShortcutInstalled(context, pwa.uuid))
    }
    
    // Card elevation animation
    val elevation by animateDpAsState(
        targetValue = if (expanded) 12.dp else 8.dp,
        label = "card_elevation",
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    )
    
    // Card scale animation on press
    var isPressed by remember { mutableStateOf(false) }
    val shadowBlurRadius by animateDpAsState(
        targetValue = if (isPressed) 12.dp else elevation,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "shadow_blur"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "card_scale",
        animationSpec = tween(100, easing = FastOutSlowInEasing)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                onClick = {
                    isPressed = true
                    launchPwa(pwa, context)
                },
                onLongClick = {
                    expanded = !expanded
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Main content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App icon with animation
                val appIcon by remember(pwa.name) { mutableStateOf(getAppIcon(pwa.name)) }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = appIcon,
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // App info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    val appIcon by remember(pwa.name) { mutableStateOf(getAppIcon(pwa.name)) }
                    Text(
                        text = pwa.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (pwa.description.isNotEmpty()) {
                        Text(
                            text = pwa.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hasIndexFile) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasIndexFile) "Ready to launch" else "Generating...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (hasIndexFile) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Expand indicator with animation
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "More options",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }

            // Expanded actions with animation
            AnimatedVisibility(
                visible = expanded,
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
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 0.dp)
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Primary actions row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Install/Uninstall button
                            if (isInstalled) {
                                FilledTonalButton(
                                    onClick = {
                                        val installer = PwaInstaller(context)
                                        installer.uninstall(pwa.uuid)
                                        isInstalled = false
                                        Toast.makeText(context, "App uninstalled successfully.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = hasIndexFile,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Uninstall",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Uninstall",
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = {
                                        val installer = PwaInstaller(context)
                                        installer.install(pwa.uuid)
                                        isInstalled = true
                                        Toast.makeText(context, "App installed successfully.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = hasIndexFile,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Install",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Install",
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Share button
                            OutlinedButton(
                                onClick = {
                                    sharePwa(pwa.name, pwaDir, context)
                                },
                                modifier = Modifier.weight(1f),
                                enabled = hasIndexFile,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.tertiary
                                ),
                                border = BorderStroke(
                                    1.5.dp,
                                    if (hasIndexFile) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = "Share",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Share",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Secondary actions row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Rework button
                            Button(
                                onClick = {
                                    navController.navigate("rework/${pwa.uuid}")
                                },
                                modifier = Modifier.weight(1f),
                                enabled = hasIndexFile,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Rework",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Rework",
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Delete button
                            TextButton(
                                onClick = onDelete,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Delete",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getAppIcon(appName: String): String {
    return when {
        appName.contains("weather", ignoreCase = true) -> "🌤️"
        appName.contains("todo", ignoreCase = true) || appName.contains("task", ignoreCase = true) -> "✅"
        appName.contains("note", ignoreCase = true) -> "📝"
        appName.contains("calculator", ignoreCase = true) -> "🧮"
        appName.contains("calendar", ignoreCase = true) -> "📅"
        appName.contains("music", ignoreCase = true) -> "🎵"
        appName.contains("game", ignoreCase = true) -> "🎮"
        appName.contains("food", ignoreCase = true) || appName.contains("recipe", ignoreCase = true) -> "🍕"
        appName.contains("shop", ignoreCase = true) || appName.contains("store", ignoreCase = true) -> "🛒"
        appName.contains("finance", ignoreCase = true) || appName.contains("money", ignoreCase = true) -> "💰"
        appName.contains("health", ignoreCase = true) || appName.contains("fitness", ignoreCase = true) -> "💪"
        appName.contains("social", ignoreCase = true) -> "👥"
        appName.contains("book", ignoreCase = true) || appName.contains("read", ignoreCase = true) -> "📚"
        else -> "📱"
    }
}

private fun isShortcutInstalled(context: Context, shortcutId: String): Boolean {
    return try {
        val shortcutManager = ShortcutManagerCompat.getDynamicShortcuts(context)
        shortcutManager.any { it.id == shortcutId }
    } catch (e: Exception) {
        false
    }
}

private fun launchPwa(pwa: PwaManager.PwaInfo, context: Context) {
    val pwaManager = PwaManager(context)
    val pwaDir = File(context.getExternalFilesDir(null), pwa.uuid)
    val hasIndexFile = File(pwaDir, "index.html").exists()
    
    if (!hasIndexFile) {
        Toast.makeText(context, "App not ready yet", Toast.LENGTH_SHORT).show()
        return
    }

    // Generate a unique port for this PWA to avoid conflicts
    val port = pwaManager.generateUniquePort(pwa.uuid)
    
    // Try to get a better name from manifest.json
    var pwaName = pwa.name
    try {
        val manifestFile = File(pwaDir, "manifest.json")
        if (manifestFile.exists()) {
            val manifestContent = manifestFile.readText()
            val manifestJson = JSONObject(manifestContent)
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
    
    // Start the PWA server
    if (pwaManager.startPwaServer(pwa.uuid, port)) {
        val intent = Intent(context, PwaViewerActivity::class.java).apply {
            putExtra("pwaUrl", "http://127.0.0.1:$port/index.html")
            putExtra("pwaName", pwaName)
            putExtra("pwaId", pwa.uuid)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "Failed to start App server", Toast.LENGTH_SHORT).show()
    }
}

private fun sharePwa(
    pwaName: String,
    pwaDir: File,
    context: Context
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            if (pwaDir.exists()) {
                val zipFileName = "${pwaName.replace("[^a-zA-Z0-9]".toRegex(), "_")}.zip"
                val zipFile = File(context.cacheDir, zipFileName)

                if (zipFile.exists()) zipFile.delete()

                val zip = ZipFile(zipFile)
                zip.addFolder(pwaDir)

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            zipFile
                        )
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "application/zip"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "App Source Code: $pwaName")
                            putExtra(Intent.EXTRA_TEXT, "Here's the source code for the App: $pwaName")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share App Source"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to share ZIP: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "App directory not found", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Failed to create ZIP: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}