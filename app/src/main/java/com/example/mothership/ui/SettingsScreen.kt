

package com.example.mothership.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mothership.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, settingsViewModel: SettingsViewModel) {
    val apiKey by settingsViewModel.apiKey.collectAsState()
    var newApiKey by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = newApiKey,
            onValueChange = { newApiKey = it },
            label = { Text("OpenRouter API Key") },
            placeholder = { Text(apiKey ?: "") },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
        Button(onClick = { 
            settingsViewModel.saveApiKey(newApiKey)
            Toast.makeText(context, "API Key Saved", Toast.LENGTH_SHORT).show()
        }) {
            Text("Save API Key")
        }
    }
}

