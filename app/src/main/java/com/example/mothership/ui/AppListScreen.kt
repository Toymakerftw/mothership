
package com.example.mothership.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Generated PWAs",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (pwas.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No PWAs generated yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn {
                items(pwas) { (uuid, pwaName) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = pwaName,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 16.dp)
                            ) {
                                IconButton(
                                    onClick = { 
                                        val pwaDir = File(context.getExternalFilesDir(null), uuid)
                                        val indexFile = File(pwaDir, "index.html")
                                        if (indexFile.exists()) {
                                            val intent = Intent(context, PwaViewerActivity::class.java)
                                            intent.putExtra("pwaUrl", "file://${context.getExternalFilesDir(null)}/$uuid/index.html")
                                            intent.putExtra("pwaName", pwaName)
                                            context.startActivity(intent)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "View PWA",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                IconButton(
                                    onClick = { 
                                        val installer = com.example.mothership.PwaInstaller(context)
                                        installer.install(uuid)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Install PWA",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                
                                IconButton(
                                    onClick = { 
                                        // Create ZIP and share
                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                val pwaDir = File(context.getExternalFilesDir(null), uuid)
                                                if (pwaDir.exists()) {
                                                    val zipFileName = "${pwaName.replace("[^a-zA-Z0-9]".toRegex(), "_")}.zip"
                                                    val zipFile = File(context.cacheDir, zipFileName)
                                                    
                                                    // Delete existing zip file if it exists
                                                    if (zipFile.exists()) {
                                                        zipFile.delete()
                                                    }
                                                    
                                                    val zip = ZipFile(zipFile)
                                                    zip.addFolder(pwaDir)
                                                    
                                                    // Share the ZIP file
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
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share PWA Source",
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                
                                IconButton(
                                    onClick = { mainViewModel.deletePwa(uuid) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete PWA",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
