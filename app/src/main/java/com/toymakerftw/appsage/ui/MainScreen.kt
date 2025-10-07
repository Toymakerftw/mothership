package com.toymakerftw.appsage.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.toymakerftw.appsage.MainViewModel
import com.toymakerftw.appsage.ModelInfo
import com.toymakerftw.appsage.data.SettingsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    navController: NavController? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val models by viewModel.models.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    
    var apiKey by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var currentModel by remember { mutableStateOf(selectedModel ?: "openai/gpt-3.5-turbo") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Appsage - AI PWA Generator",
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
        
        // Model Selection
        if (models.isNotEmpty()) {
            Text(
                text = "Select Model:",
                style = MaterialTheme.typography.titleMedium
            )
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = currentModel,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth(),
                    trailingIcon = { 
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                    }
                )
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    models.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model.name) },
                            onClick = {
                                currentModel = model.id
                                viewModel.setSelectedModel(model.id)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        
        // Prompt Input
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Describe your PWA") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = { Text("e.g., A weather dashboard with dark mode") }
        )
        
        // Generate Button
        Button(
            onClick = { 
                // Save API key to settings and generate PWA - moved to viewModel
                viewModel.saveApiKeyAndGeneratePwa(apiKey, prompt)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.isGenerating.not()
        ) {
            Text(if (uiState.isGenerating) "Generating..." else "Generate PWA")
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
        
        if (uiState.pwaGenerated) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "PWA Generated Successfully! UUID: ${uiState.pwaUuid}",
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}