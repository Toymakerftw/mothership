package com.example.mothership.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mothership.ChatMessage
import com.example.mothership.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToApps: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    val isGenerating by viewModel.isGenerating.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val messages by viewModel.chatMessages.collectAsState()
    val lazyListState = rememberLazyListState()

    // Auto-scroll to bottom when new messages are added
    LaunchedEffect(messages.size) {
        lazyListState.animateScrollToItem(messages.size)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Mothership",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        
        Text(
            text = "Generate Progressive Web Apps with AI",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        
        // Chat container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Welcome message
                if (messages.isEmpty()) {
                    item {
                        ChatBubble(
                            message = ChatMessage(
                                id = 0,
                                text = "Welcome to Mothership! Describe the PWA you'd like to create and I'll help you build it.",
                                isUser = false
                            )
                        )
                    }
                }
                
                // Chat messages
                items(messages) { message ->
                    ChatBubble(message = message)
                }
                
                // Progress indicator when generating
                if (isGenerating) {
                    item {
                        GeneratingIndicator()
                    }
                }
            }
        }
        
        // Progress bar
        if (isGenerating) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
        
        // Input area
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Describe your app idea") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            placeholder = { Text("e.g., A todo app with dark mode") },
            enabled = !isGenerating
        )
        
        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Generate button
            Button(
                onClick = {
                    if (prompt.isNotBlank() && !isGenerating) {
                        viewModel.sendMessage(prompt)
                        // Clear input
                        prompt = ""
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = prompt.isNotBlank() && !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Create, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                }
                Text(if (isGenerating) "Generating..." else "Generate PWA")
            }
            
            // View Apps button
            Button(
                onClick = onNavigateToApps,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Icon(Icons.Filled.ViewList, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("View Apps")
            }
        }
        
        // Settings button
        Button(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = ButtonDefaults.textButtonColors()
        ) {
            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Settings")
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (message.isUser) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                color = if (message.isUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun GeneratingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 2.dp
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "Generating your PWA...",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}