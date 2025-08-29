package com.example.mothership.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mothership.PwaViewerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalFoundationApi::class)
data class PwaApp(
    val id: String,
    val name: String,
    val created: String,
    val path: String
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    onBackClicked: () -> Unit,
    onSettingsClicked: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pwaApps by remember { mutableStateOf<List<PwaApp>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showContextMenu by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<PwaApp?>(null) }

    // Load PWA apps
    LaunchedEffect(Unit) {
        pwaApps = loadPwaApps(context)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generated PWAs") },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClicked) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (pwaApps.isEmpty()) {
            EmptyAppList(padding)
        } else {
            PwaAppList(
                pwaApps = pwaApps,
                padding = padding,
                onAppClicked = { app ->
                    openPwaApp(context, app)
                },
                onAppLongClicked = { app ->
                    selectedApp = app
                    showContextMenu = true
                }
            )
        }

        // Context menu for PWA actions
        if (showContextMenu && selectedApp != null) {
            PwaContextMenu(
                onDismiss = {
                    showContextMenu = false
                    selectedApp = null
                },
                onDelete = {
                    selectedApp?.let { app ->
                        scope.launch {
                            val success = deletePwaApp(context, app)
                            if (success) {
                                // Refresh the list
                                pwaApps = loadPwaApps(context)
                                Toast.makeText(context, "PWA deleted", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to delete PWA", Toast.LENGTH_SHORT).show()
                            }
                            showContextMenu = false
                            selectedApp = null
                        }
                    }
                },
                onExport = {
                    selectedApp?.let { app ->
                        scope.launch {
                            val success = exportPwaApp(context, app)
                            if (success) {
                                Toast.makeText(context, "PWA exported to Downloads", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to export PWA", Toast.LENGTH_SHORT).show()
                            }
                            showContextMenu = false
                            selectedApp = null
                        }
                    }
                },
                onInstall = {
                    selectedApp?.let { app ->
                        installPwaApp(context, app)
                        showContextMenu = false
                        selectedApp = null
                    }
                }
            )
        }
    }
}

@Composable
fun EmptyAppList(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Apps,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No PWAs generated yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Generate your first PWA using the chat interface",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PwaAppList(
    pwaApps: List<PwaApp>,
    padding: PaddingValues,
    onAppClicked: (PwaApp) -> Unit,
    onAppLongClicked: (PwaApp) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
    ) {
        items(pwaApps) { app ->
            PwaAppItem(
                app = app,
                onAppClicked = onAppClicked,
                onAppLongClicked = onAppLongClicked
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PwaAppItem(
    app: PwaApp,
    onAppClicked: (PwaApp) -> Unit,
    onAppLongClicked: (PwaApp) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onAppClicked(app) },
                onLongClick = { onAppLongClicked(app) }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Android,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDateString(app.created),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Open"
            )
        }
    }
}

@Composable
fun PwaContextMenu(
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onInstall: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PWA Options") },
        text = {
            Column {
                Button(
                    onClick = onInstall,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Install PWA")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onExport,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Export PWA")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Delete PWA")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun openPwaApp(context: Context, app: PwaApp) {
    val intent = Intent(context, PwaViewerActivity::class.java).apply {
        putExtra("pwa_path", app.path)
        putExtra("pwa_name", app.name)
    }
    context.startActivity(intent)
}

private fun installPwaApp(context: Context, app: PwaApp) {
    try {
        // Create an intent to launch the PWA as a standalone app
        val intent = Intent(context, PwaViewerActivity::class.java).apply {
            putExtra("pwa_path", app.path)
            putExtra("pwa_name", app.name)
            putExtra("install_mode", true) // Flag to indicate this is an installation
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        // Create a shortcut/installation intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For Android 8.0 and above, we can create a shortcut
            createShortcut(context, app, intent)
        } else {
            // For older versions, just show a message
            Toast.makeText(context, "PWA ready to launch. Long-press app icon to add to home screen.", Toast.LENGTH_LONG).show()
        }
        
        Log.d("AppListScreen", "PWA installation initiated for: ${app.name}")
    } catch (e: Exception) {
        Log.e("AppListScreen", "Error installing PWA", e)
        Toast.makeText(context, "Failed to install PWA: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun createShortcut(context: Context, app: PwaApp, launchIntent: Intent) {
    try {
        // Create a shortcut for the PWA
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        
        if (shortcutManager.isRequestPinShortcutSupported) {
            val shortcut = ShortcutInfo.Builder(context, app.id)
                .setShortLabel(app.name)
                .setLongLabel(app.name)
                .setIcon(Icon.createWithResource(context, android.R.drawable.ic_menu_share)) // Use system icon
                .setIntent(launchIntent)
                .build()
            
            shortcutManager.requestPinShortcut(shortcut, null)
            Toast.makeText(context, "PWA installed as shortcut", Toast.LENGTH_SHORT).show()
        } else {
            // Fallback: Launch the PWA in standalone mode
            context.startActivity(launchIntent)
            Toast.makeText(context, "PWA launched. Use 'Add to Home screen' in browser menu to install.", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Log.e("AppListScreen", "Error creating shortcut", e)
        // Fallback: Launch the PWA
        context.startActivity(launchIntent)
        Toast.makeText(context, "PWA launched. Use 'Add to Home screen' in browser menu to install.", Toast.LENGTH_LONG).show()
    }
}

private suspend fun deletePwaApp(context: Context, app: PwaApp): Boolean = withContext(Dispatchers.IO) {
    try {
        val appDir = File(app.path)
        if (appDir.exists() && appDir.isDirectory) {
            // Delete the directory recursively
            appDir.deleteRecursively()
            return@withContext true
        } else {
            return@withContext false
        }
    } catch (e: Exception) {
        Log.e("AppListScreen", "Error deleting PWA", e)
        return@withContext false
    }
}

private suspend fun exportPwaApp(context: Context, app: PwaApp): Boolean = withContext(Dispatchers.IO) {
    try {
        val appDir = File(app.path)
        if (appDir.exists() && appDir.isDirectory) {
            // Create zip file in Downloads directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val zipFile = File(downloadsDir, "${app.name.replace(" ", "_")}.zip")
            
            // Create zip file
            ZipOutputStream(zipFile.outputStream()).use { zos ->
                appDir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val relativePath = file.relativeTo(appDir).path
                        val entry = ZipEntry(relativePath)
                        zos.putNextEntry(entry)
                        file.inputStream().use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }
            
            // Notify system of new file
            val uri = Uri.fromFile(zipFile)
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                data = uri
            }
            context.sendBroadcast(intent)
            
            return@withContext true
        } else {
            return@withContext false
        }
    } catch (e: Exception) {
        Log.e("AppListScreen", "Error exporting PWA", e)
        return@withContext false
    }
}

private suspend fun loadPwaApps(context: Context): List<PwaApp> = withContext(Dispatchers.IO) {
    val pwaApps = mutableListOf<PwaApp>()
    val mothershipDir = File(context.getExternalFilesDir(null)?.parentFile, "Mothership/PWAs")
    
    if (mothershipDir.exists() && mothershipDir.isDirectory) {
        mothershipDir.listFiles()?.forEach { appDir ->
            if (appDir.isDirectory) {
                val infoFile = File(appDir, "app_info.json")
                if (infoFile.exists()) {
                    try {
                        val infoContent = infoFile.readText()
                        // Simple JSON parsing (in a real app, you'd use a proper JSON library)
                        val id = extractJsonValue(infoContent, "id")
                        val name = extractJsonValue(infoContent, "name")
                        val created = extractJsonValue(infoContent, "created")
                        
                        if (id != null && name != null && created != null) {
                            pwaApps.add(
                                PwaApp(
                                    id = id,
                                    name = name,
                                    created = created,
                                    path = appDir.absolutePath
                                )
                            )
                        }
                    } catch (e: Exception) {
                        // Ignore invalid app info files
                    }
                }
            }
        }
    }
    
    return@withContext pwaApps
}

private fun extractJsonValue(json: String, key: String): String? {
    val pattern = "\"$key\"\\s*:\\s*\"([^\"]+)\"".toRegex()
    return pattern.find(json)?.groupValues?.getOrNull(1)
}

private fun formatDateString(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}