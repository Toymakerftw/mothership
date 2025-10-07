package com.toymakerftw.appsage.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toymakerftw.appsage.ReworkViewModel
import com.toymakerftw.appsage.data.SettingsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReworkScreen(
    uuid: String,
    viewModel: ReworkViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var apiKey by remember { mutableStateOf("") }
    var reworkPrompt by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Rework PWA (UUID: $uuid)",
            style = MaterialTheme.typography.headlineMedium
        )
        
        // API Key Input
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("OpenRouter API Key") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        
        // Rework Prompt Input
        OutlinedTextField(
            value = reworkPrompt,
            onValueChange = { reworkPrompt = it },
            label = { Text("Describe changes to make") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = { Text("e.g., Add a dark mode toggle, make the header sticky, or change the color scheme") }
        )
        
        // Rework Button
        Button(
            onClick = { 
                // Save API key to settings and rework PWA - moved to viewModel
                viewModel.saveApiKeyAndReworkPwa(apiKey, uuid, reworkPrompt)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.isReworking.not()
        ) {
            Text(if (uiState.isReworking) "Reworking..." else "Apply Changes")
        }
        
        // Status messages
        if (uiState.errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        if (uiState.pwaReworked) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "PWA Reworked Successfully!",
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}