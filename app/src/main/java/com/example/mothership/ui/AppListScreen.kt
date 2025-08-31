package com.example.mothership.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mothership.MainViewModel
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
            .padding(24.dp)
    ) {
        Text(
            text = "Generated PWAs",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Your collection of AI-generated Progressive Web Apps",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (pwas.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "No apps",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No PWAs generated yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = "Go to the home screen to create your first PWA!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    AnimatedButton(
                        onClick = { navController.navigate("main") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = "Create Your First PWA",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            LazyColumn {
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "App Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pwaName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (hasIndexFile) "Tap to open" else "Generated",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasIndexFile) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // More actions on long press
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ActionButton(
                            icon = Icons.Default.Add,
                            label = "Install",
                            color = MaterialTheme.colorScheme.secondary
                        ) {
                            val installer = com.example.mothership.PwaInstaller(context)
                            installer.install(uuid)
                        }

                        ActionButton(
                            icon = Icons.Default.Share,
                            label = "Share",
                            color = MaterialTheme.colorScheme.tertiary
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
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            border = BorderStroke(2.dp, color),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) color else color.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) color else color.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
