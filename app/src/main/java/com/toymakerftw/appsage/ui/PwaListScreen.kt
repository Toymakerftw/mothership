package com.toymakerftw.appsage.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toymakerftw.appsage.service.PwaManager
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.toymakerftw.appsage.PwaInstaller
import java.io.File

@Composable
fun PwaListScreen(context: Context) {
    val pwaManager = remember { PwaManager(context) }
    var pwas by remember { mutableStateOf(emptyList<PwaManager.PwaInfo>()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        pwas = pwaManager.getGeneratedPwas()
        isLoading = false
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Generated PWAs",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (pwas.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No PWAs generated yet",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pwas) { pwa ->
                    PwaItemCard(
                        pwa = pwa,
                        context = context,
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

@Composable
fun PwaItemCard(
    pwa: PwaManager.PwaInfo,
    context: Context,
    onDelete: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onLaunch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        style = MaterialTheme.typography.titleMedium
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
                
                // Action buttons
                Row {
                    IconButton(onClick = onLaunch) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Launch")
                    }
                    IconButton(onClick = onInstall) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Install")
                    }
                    IconButton(onClick = onUninstall) {
                        Icon(Icons.Default.Delete, contentDescription = "Uninstall")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}