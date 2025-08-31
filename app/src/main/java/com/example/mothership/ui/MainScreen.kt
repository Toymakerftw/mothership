
package com.example.mothership.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mothership.MainViewModel
import com.example.mothership.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, mainViewModel: MainViewModel) {
    var prompt by remember { mutableStateOf("") }
    val uiState by mainViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Describe your PWA") },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
        Button(onClick = { 
            if (prompt.isNotBlank()) {
                mainViewModel.generatePwa(prompt)
            }
        }) {
            Text("Generate PWA")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("appList") }) {
            Text("View Generated PWAs")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("settings") }) {
            Text("Settings")
        }

        when (uiState) {
            is MainUiState.Loading -> CircularProgressIndicator()
            is MainUiState.Error -> Text((uiState as MainUiState.Error).message)
            else -> {}
        }
    }
}
