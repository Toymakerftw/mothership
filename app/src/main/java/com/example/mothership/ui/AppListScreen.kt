package com.example.mothership.ui

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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.navigation.NavController
import com.example.mothership.MainViewModel
import com.example.mothership.PwaInstaller
import com.example.mothership.PwaViewerActivity
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
    val context = LocalContext.current

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
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🚀", fontSize = 24.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Your PWA Collection",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "${pwas.size} ${if (pwas.size == 1) "app" else "apps"} generated",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (pwas.isEmpty()) {
            // Empty state
            Card(
                modifier = Modifier.fillMaxSize(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                                fontSize = 48.sp
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
                            text = "You haven't created any PWAs yet. Start building your first app and watch the magic happen!",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 12.dp, bottom = 32.dp),
                            lineHeight = 22.sp
                        )
                        
                        AnimatedButton(
                            onClick = { navController.navigate("main") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(text = "🚀", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Create Your First PWA",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            // PWA list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(pwas) { (uuid, pwaName) ->
                    AppCard(
                        uuid = uuid,
                        pwaName = pwaName,
                        mainViewModel = mainViewModel
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) } // Space for bottom nav
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppCard(
    uuid: String,
    pwaName: String,
    mainViewModel: MainViewModel
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
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp),
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
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App icon
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
                        text = "📱",
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // App info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = pwaName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
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

                // Expand indicator
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Expand",
                    modifier = Modifier
                        .size(24.dp)
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
                        .padding(horizontal = 24.dp, vertical = 0.dp)
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (isInstalled) {
                            ActionButton(
                                icon = Icons.Outlined.Delete,
                                label = "Uninstall",
                                color = MaterialTheme.colorScheme.error,
                                enabled = hasIndexFile
                            ) {
                                val installer = PwaInstaller(context)
                                installer.uninstall(uuid)
                                isInstalled = false
                                Toast.makeText(context, "PWA uninstalled successfully.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            ActionButton(
                                icon = Icons.Outlined.GetApp,
                                label = "Install",
                                color = MaterialTheme.colorScheme.secondary,
                                enabled = hasIndexFile
                            ) {
                                val installer = PwaInstaller(context)
                                installer.install(uuid)
                                isInstalled = true
                                Toast.makeText(context, "PWA installed successfully.", Toast.LENGTH_SHORT).show()
                            }
                        }

                        ActionButton(
                            icon = Icons.Outlined.Share,
                            label = "Share",
                            color = MaterialTheme.colorScheme.tertiary,
                            enabled = hasIndexFile
                        ) {
                            sharePwa(uuid, pwaName, pwaDir, context)
                        }

                        ActionButton(
                            icon = Icons.Default.Delete,
                            label = "Delete",
                            color = MaterialTheme.colorScheme.error
                        ) {
                            mainViewModel.deletePwa(uuid)
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
    uuid: String,
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

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                width = 2.dp, 
                color = if (enabled) color else color.copy(alpha = 0.3f)
            ),
            contentPadding = PaddingValues(0.dp),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                containerColor = if (enabled) color.copy(alpha = 0.1f) else Color.Transparent
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) color else color.copy(alpha = 0.5f),
                modifier = Modifier.size(28.dp)
            )
        }
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) color else color.copy(alpha = 0.5f),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}