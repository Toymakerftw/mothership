package com.toymakerftw.mothership.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.navigation.NavController
import com.toymakerftw.mothership.MainViewModel
import com.toymakerftw.mothership.PwaInstaller
import com.toymakerftw.mothership.PwaViewerActivity
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.lingala.zip4j.ZipFile
import android.content.Intent.ACTION_SEND
import android.net.Uri
import androidx.core.content.FileProvider

@Composable
fun AppListScreen(navController: NavController, mainViewModel: MainViewModel) {
    val pwas by mainViewModel.pwas.collectAsState()
    mainViewModel.getPwas()
    
    // Get screen configuration for responsive sizing
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isTablet = screenWidth > 600
    
    // Calculate responsive sizes
    val padding = if (isTablet) 40.dp else 20.dp
    val headerPadding = if (isTablet) 36.dp else 24.dp
    val headerIconSize = if (isTablet) 80.dp else 60.dp
    val headerIconTextSize = if (isTablet) 32.sp else 24.sp
    val emptyStateIconSize = if (isTablet) 160.dp else 120.dp
    val emptyStateIconTextSize = if (isTablet) 64.sp else 48.sp
    val cardCornerRadius = if (isTablet) 32.dp else 24.dp
    val emptyStateCardCornerRadius = if (isTablet) 36.dp else 24.dp
    val emptyStatePadding = if (isTablet) 48.dp else 32.dp
    val buttonHeight = if (isTablet) 64.dp else 56.dp
    val buttonCornerRadius = if (isTablet) 20.dp else 16.dp

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
            .padding(padding)
    ) {
        // Header
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
                text = "Your PWA Collection",
                style = if (isTablet) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "${pwas.size} ${if (pwas.size == 1) "app" else "apps"} generated",
                style = if (isTablet) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = if (isTablet) 8.dp else 4.dp)
            )
        }

        if (pwas.isEmpty()) {
            // Empty state
            Card(
                modifier = Modifier.fillMaxSize(),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isTablet) 12.dp else 8.dp),
                shape = RoundedCornerShape(emptyStateCardCornerRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(emptyStatePadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(emptyStateIconSize)
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
                                fontSize = emptyStateIconTextSize
                            )
                        }

                        Spacer(modifier = Modifier.height(if (isTablet) 32.dp else 24.dp))

                        Text(
                            text = "Ready for Launch!",
                            style = if (isTablet) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "You haven't created any PWAs yet. Start building your first app and watch the magic happen!",
                            style = if (isTablet) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = if (isTablet) 16.dp else 12.dp, bottom = if (isTablet) 48.dp else 32.dp),
                            lineHeight = if (isTablet) 28.sp else 22.sp
                        )

                        Button(
                            onClick = { navController.navigate("main") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(buttonHeight),
                            shape = RoundedCornerShape(buttonCornerRadius),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(text = "🚀", fontSize = if (isTablet) 28.sp else 20.sp)
                            Spacer(modifier = Modifier.width(if (isTablet) 12.dp else 8.dp))
                            Text(
                                text = "Create Your First PWA",
                                style = if (isTablet) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            // PWA list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(if (isTablet) 24.dp else 16.dp)
            ) {
                items(pwas) { (uuid, pwaName) ->
                    AppCard(
                        uuid = uuid,
                        pwaName = pwaName,
                        mainViewModel = mainViewModel,
                        navController = navController,
                        isTablet = isTablet
                    )
                }
                item { Spacer(modifier = Modifier.height(if (isTablet) 120.dp else 80.dp)) } // Space for bottom nav
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppCard(
    uuid: String,
    pwaName: String,
    mainViewModel: MainViewModel,
    navController: NavController,
    isTablet: Boolean = false
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val pwaDir = remember { File(context.getExternalFilesDir(null), uuid) }
    val hasIndexFile = remember(pwaDir) { File(pwaDir, "index.html").exists() }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "expand_rotation"
    )
    var isInstalled by remember(uuid) {
        mutableStateOf(isShortcutInstalled(context, uuid))
    }
    
    // Calculate responsive sizes
    val cardPadding = if (isTablet) 32.dp else 24.dp
    val cardCornerRadius = if (isTablet) 28.dp else 20.dp
    val appIconSize = if (isTablet) 72.dp else 56.dp
    val appIconTextSize = if (isTablet) 32.sp else 24.sp
    val iconSize = if (isTablet) 32.dp else 24.dp
    val expandedCardPadding = if (isTablet) 28.dp else 20.dp
    val expandedCardCornerRadius = if (isTablet) 20.dp else 16.dp
    val buttonCornerRadius = if (isTablet) 16.dp else 12.dp
    val buttonIconSize = if (isTablet) 24.dp else 18.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (hasIndexFile) {
                        val intent = Intent(context, PwaViewerActivity::class.java).apply {
                            putExtra("pwaUrl", "file://${context.getExternalFilesDir(null)}/$uuid/index.html")
                            putExtra("pwaName", pwaName)
                        }
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "PWA not ready yet", Toast.LENGTH_SHORT).show()
                    }
                },
                onLongClick = {
                    expanded = !expanded
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isTablet) 12.dp else 8.dp),
        shape = RoundedCornerShape(cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Main content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App icon
                Box(
                    modifier = Modifier
                        .size(appIconSize)
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
                        text = "📱",
                        fontSize = appIconTextSize
                    )
                }

                Spacer(modifier = Modifier.width(if (isTablet) 24.dp else 16.dp))

                // App info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = pwaName,
                        style = if (isTablet) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = if (isTablet) 8.dp else 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isTablet) 12.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hasIndexFile) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                )
                        )
                        Spacer(modifier = Modifier.width(if (isTablet) 12.dp else 8.dp))
                        Text(
                            text = if (hasIndexFile) "Ready to launch" else "Generating...",
                            style = if (isTablet) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (hasIndexFile) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Expand indicator
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Expand",
                    modifier = Modifier
                        .size(iconSize)
                        .rotate(rotationAngle),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }

            // Expanded actions
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = cardPadding, vertical = 0.dp)
                        .padding(bottom = cardPadding),
                    shape = RoundedCornerShape(expandedCardCornerRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(expandedCardPadding),
                        verticalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)
                    ) {
                        // Primary actions row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)
                        ) {
                            // Install/Uninstall button
                            if (isInstalled) {
                                FilledTonalButton(
                                    onClick = {
                                        val installer = PwaInstaller(context)
                                        installer.uninstall(uuid)
                                        isInstalled = false
                                        Toast.makeText(context, "PWA uninstalled successfully.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = hasIndexFile,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    shape = RoundedCornerShape(buttonCornerRadius)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(buttonIconSize)
                                    )
                                    Spacer(modifier = Modifier.width(if (isTablet) 12.dp else 8.dp))
                                    Text(
                                        text = "Uninstall",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = if (isTablet) 16.sp else 14.sp
                                    )
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = {
                                        val installer = PwaInstaller(context)
                                        installer.install(uuid)
                                        isInstalled = true
                                        Toast.makeText(context, "PWA installed successfully.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = hasIndexFile,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    shape = RoundedCornerShape(buttonCornerRadius)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.GetApp,
                                        contentDescription = null,
                                        modifier = Modifier.size(buttonIconSize)
                                    )
                                    Spacer(modifier = Modifier.width(if (isTablet) 12.dp else 8.dp))
                                    Text(
                                        text = "Install",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = if (isTablet) 16.sp else 14.sp
                                    )
                                }
                            }

                            // Share button
                            OutlinedButton(
                                onClick = {
                                    sharePwa(pwaName, pwaDir, context)
                                },
                                modifier = Modifier.weight(1f),
                                enabled = hasIndexFile,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.tertiary
                                ),
                                border = BorderStroke(
                                    if (isTablet) 2.dp else 1.5.dp,
                                    if (hasIndexFile) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(buttonCornerRadius)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(buttonIconSize)
                                )
                                Spacer(modifier = Modifier.width(if (isTablet) 12.dp else 8.dp))
                                Text(
                                    text = "Share",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = if (isTablet) 16.sp else 14.sp
                                )
                            }
                        }

                        // Secondary actions row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)
                        ) {
                            // Rework button
                            Button(
                                onClick = {
                                    navController.navigate("rework/$uuid/$pwaName")
                                },
                                modifier = Modifier.weight(1f),
                                enabled = hasIndexFile,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(buttonCornerRadius)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(buttonIconSize)
                                )
                                Spacer(modifier = Modifier.width(if (isTablet) 12.dp else 8.dp))
                                Text(
                                    text = "Rework",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = if (isTablet) 16.sp else 14.sp
                                )
                            }

                            // Delete button
                            TextButton(
                                onClick = {
                                    mainViewModel.deletePwa(uuid)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(buttonCornerRadius)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(buttonIconSize)
                                )
                                Spacer(modifier = Modifier.width(if (isTablet) 12.dp else 8.dp))
                                Text(
                                    text = "Delete",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = if (isTablet) 16.sp else 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isShortcutInstalled(context: Context, shortcutId: String): Boolean {
    val shortcutManager = ShortcutManagerCompat.getDynamicShortcuts(context)
    return shortcutManager.any { it.id == shortcutId }
}

private fun sharePwa(
    pwaName: String,
    pwaDir: File,
    context: android.content.Context
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
                            action = ACTION_SEND
                            type = "application/zip"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "PWA Source Code: $pwaName")
                            putExtra(Intent.EXTRA_TEXT, "Here's the source code for the PWA: $pwaName")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share PWA Source"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to share ZIP: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "PWA directory not found", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Failed to create ZIP: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}