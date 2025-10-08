package com.toymakerftw.appsage.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import org.json.JSONObject
import java.io.File

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

    val pwaDir = remember { File(context.getExternalFilesDir(null), uuid) }
    val isAppReady = remember { File(pwaDir, "index.html").exists() }

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
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        // 🔹 Top Bar with Back Button
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                color = MaterialTheme.colorScheme.primary
            )
        }

        // 🔸 Simple Header Text
        Text(
            text = "Modify your app by providing clear instructions. You can preview after reworking.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        // 🔸 UUID Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            )
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

        // 🔸 Rework Prompt
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Modification Instructions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Describe the changes you want",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = !uiState.isReworking,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }

        // 🔸 Action Buttons
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
                enabled = reworkPrompt.isNotBlank() && !uiState.isReworking,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (uiState.isReworking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Reworking...", fontWeight = FontWeight.Bold)
                } else {
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
            }

            // Single Preview/View App button with dynamic text and icon
            Button(
                onClick = { 
                    launchPreview(uuid, context, pwaManager)
                    isPreviewing = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = isAppReady && !uiState.isReworking,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.pwaReworked) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.secondary
                )
            ) {
                val buttonText = if (uiState.pwaReworked) "View Updated App" else "Preview App"
                val buttonIcon = if (uiState.pwaReworked) Icons.Default.PlayArrow else Icons.Default.PlayArrow
                
                Icon(
                    imageVector = buttonIcon,
                    contentDescription = buttonText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 🔸 Status Messages
        AnimatedVisibility(
            visible = uiState.errorMessage != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            uiState.errorMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // 🔸 Success Message - simplified without redundant button
        AnimatedVisibility(
            visible = uiState.pwaReworked,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
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