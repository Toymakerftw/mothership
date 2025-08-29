package com.example.mothership.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.mothership.SettingsViewModel
import com.example.mothership.ui.theme.LocalThemeManager
import com.example.mothership.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigateBack: () -> Unit) {
    val apiKey by viewModel.apiKey.collectAsState()
    var apiKeyInput by remember { mutableStateOf(apiKey) }
    val themeManager = LocalThemeManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Icon(
            imageVector = Icons.Default.Key,
            contentDescription = null,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .height(48.dp)
        )
        
        Text(
            text = "Settings",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = { apiKeyInput = it },
            label = { Text("OpenRouter API Key") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            supportingText = {
                Text("Get your API key from https://openrouter.ai/")
            }
        )
        
        Button(
            onClick = {
                viewModel.saveApiKey(apiKeyInput)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Save API Key")
        }
        
        if (apiKeyInput.isNotEmpty()) {
            Text(
                text = "API key is set",
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        // Theme Selection
        ThemeSelection(themeManager)
        
        InfoCard()
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ThemeSelection(themeManager: com.example.mothership.ui.theme.ThemeManager) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        androidx.compose.material3.Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                androidx.compose.material3.Card(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .align(Alignment.Start),
                    shape = androidx.compose.material3.MaterialTheme.shapes.extraSmall
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(4.dp)
                            .height(16.dp)
                    )
                }
                
                Text(
                    text = "Theme",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Theme options
                androidx.compose.material3.RadioButton(
                    selected = themeManager.themeMode.value == ThemeMode.LIGHT,
                    onClick = { themeManager.setThemeMode(ThemeMode.LIGHT) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
                Text(
                    text = "Light",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(start = 32.dp)
                        .padding(bottom = 8.dp)
                )
                
                androidx.compose.material3.RadioButton(
                    selected = themeManager.themeMode.value == ThemeMode.DARK,
                    onClick = { themeManager.setThemeMode(ThemeMode.DARK) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
                Text(
                    text = "Dark",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(start = 32.dp)
                        .padding(bottom = 8.dp)
                )
                
                androidx.compose.material3.RadioButton(
                    selected = themeManager.themeMode.value == ThemeMode.SYSTEM,
                    onClick = { themeManager.setThemeMode(ThemeMode.SYSTEM) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
                Text(
                    text = "System Default",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(start = 32.dp)
                        .padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
fun InfoCard() {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            androidx.compose.material3.Card(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .align(Alignment.Start),
                shape = androidx.compose.material3.MaterialTheme.shapes.extraSmall
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(4.dp)
                        .height(16.dp)
                )
            }
            
            Text(
                text = "About OpenRouter",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "OpenRouter is a unified interface for LLMs. It provides access to models from OpenAI, Anthropic, Google, and more through a single API.",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "To get your API key:",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = "1. Visit https://openrouter.ai/",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "2. Sign up for an account",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "3. Go to Settings > API Keys",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "4. Create a new API key",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "5. Copy and paste it above",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
        }
    }
}