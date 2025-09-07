package com.toymakerftw.mothership.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.toymakerftw.mothership.MainUiState
import com.toymakerftw.mothership.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReworkScreen(
    navController: NavController,
    mainViewModel: MainViewModel,
    pwaUuid: String,
    pwaName: String
) {
    val prompt by mainViewModel.prompt.collectAsState()
    val uiState by mainViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    // Get screen configuration for responsive sizing
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isTablet = screenWidth > 600
    
    // Calculate responsive sizes
    val padding = if (isTablet) 40.dp else 20.dp
    val headerPadding = if (isTablet) 48.dp else 32.dp
    val logoSize = if (isTablet) 100.dp else 80.dp
    val logoIconSize = if (isTablet) 56.dp else 40.dp
    val cardPadding = if (isTablet) 36.dp else 28.dp
    val textFieldHeight = if (isTablet) 160.dp else 120.dp
    val buttonHeight = if (isTablet) 64.dp else 56.dp
    val cardCornerRadius = if (isTablet) 32.dp else 24.dp
    val quickActionCornerRadius = if (isTablet) 24.dp else 20.dp
    val quickActionButtonHeight = if (isTablet) 80.dp else 72.dp

    val isLoading = uiState is MainUiState.Loading

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
            .verticalScroll(scrollState)
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = { navController.navigate("appList") }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(if (isTablet) 32.dp else 24.dp))
            }
        }
        // Header with rocket logo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = headerPadding)
        ) {
            Box(
                modifier = Modifier
                    .size(logoSize)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Rework",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(logoIconSize)
                )
            }
            
            Spacer(modifier = Modifier.height(if (isTablet) 24.dp else 16.dp))
            
            Text(
                text = "Rework $pwaName",
                style = if (isTablet) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            
            Text(
                text = "Modify your existing Progressive Web App",
                style = if (isTablet) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = if (isTablet) 8.dp else 4.dp)
            )
        }

        // Main rework card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isTablet) 36.dp else 24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isTablet) 16.dp else 12.dp),
            shape = RoundedCornerShape(cardCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = if (isTablet) 30.dp else 20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = "Rework",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (isTablet) 36.dp else 28.dp)
                    )
                    Spacer(modifier = Modifier.width(if (isTablet) 16.dp else 12.dp))
                    Text(
                        text = "Modify Your PWA",
                        style = if (isTablet) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Describe what changes you'd like to make to your existing app. The AI will modify the existing code based on your instructions.",
                    style = if (isTablet) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = if (isTablet) 36.dp else 28.dp),
                    lineHeight = if (isTablet) 28.sp else 24.sp
                )

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { mainViewModel.setPrompt(it) },
                    label = { 
                        Text(
                            "What changes would you like to make?",
                            fontWeight = FontWeight.Medium,
                            fontSize = if (isTablet) 18.sp else 16.sp
                        ) 
                    },
                    placeholder = { 
                        Text(
                            "e.g., Add a dark mode toggle, or change the color scheme...",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = if (isTablet) 16.sp else 14.sp
                        ) 
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(textFieldHeight)
                        .padding(bottom = if (isTablet) 32.dp else 24.dp),
                    shape = RoundedCornerShape(if (isTablet) 20.dp else 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    maxLines = 4
                )

                LoadingButton(
                    onClick = {
                        if (prompt.isNotBlank()) {
                            mainViewModel.reworkPwa(pwaUuid, pwaName, prompt)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                    enabled = prompt.isNotBlank() && !isLoading,
                    isLoading = isLoading,
                    shape = RoundedCornerShape(if (isTablet) 20.dp else 16.dp),
                    normalText = "Apply Changes to PWA",
                    fontSize = if (isTablet) 18.sp else 16.sp
                )

                // Error state with retry functionality
                when (uiState) {
                    is MainUiState.Error -> {
                        Spacer(modifier = Modifier.height(if (isTablet) 30.dp else 20.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(if (isTablet) 16.dp else 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(if (isTablet) 24.dp else 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = if (isTablet) 18.dp else 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(if (isTablet) 28.dp else 20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(if (isTablet) 12.dp else 8.dp))
                                    Text(
                                        text = "Modification Failed",
                                        color = MaterialTheme.colorScheme.error,
                                        style = if (isTablet) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Text(
                                    text = (uiState as MainUiState.Error).message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = if (isTablet) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = if (isTablet) 24.dp else 16.dp)
                                )
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)
                                ) {
                                    if ((uiState as MainUiState.Error).canRetry) {
                                        androidx.compose.material3.OutlinedButton(
                                            onClick = { mainViewModel.retryGeneration() },
                                            enabled = prompt.isNotBlank(),
                                            modifier = Modifier.weight(1f),
                                            content = {
                                                Text(
                                                    "Retry",
                                                    fontSize = if (isTablet) 16.sp else 14.sp
                                                )
                                            }
                                        )
                                    } else {
                                        androidx.compose.material3.OutlinedButton(
                                            onClick = { navController.navigate("settings") },
                                            modifier = Modifier.weight(1f),
                                            content = {
                                                Text(
                                                    "Settings",
                                                    fontSize = if (isTablet) 16.sp else 14.sp
                                                )
                                            }
                                        )
                                    }
                                    
                                    androidx.compose.material3.TextButton(
                                        onClick = { mainViewModel.clearUiState() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            "Dismiss",
                                            fontSize = if (isTablet) 16.sp else 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is MainUiState.Success -> {
                        LaunchedEffect(uiState) {
                            delay(3000)
                            mainViewModel.clearUiState()
                        }
                        Spacer(modifier = Modifier.height(if (isTablet) 30.dp else 20.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(if (isTablet) 16.dp else 12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(if (isTablet) 20.dp else 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                                    contentDescription = "Success",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(if (isTablet) 32.dp else 24.dp)
                                )
                                Spacer(modifier = Modifier.width(if (isTablet) 12.dp else 8.dp))
                                Text(
                                    text = "PWA modified successfully!",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = if (isTablet) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        // Quick actions section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isTablet) 12.dp else 8.dp),
            shape = RoundedCornerShape(quickActionCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isTablet) 32.dp else 24.dp)
            ) {
                Text(
                    text = "Quick Actions",
                    style = if (isTablet) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = if (isTablet) 28.dp else 20.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)
                ) {
                    ReworkQuickActionButton(
                        icon = Icons.Default.Info,
                        title = "View Generated PWAs",
                        subtitle = "Browse your app collection",
                        onClick = { navController.navigate("appList") },
                        height = quickActionButtonHeight,
                        iconSize = if (isTablet) 32.dp else 24.dp,
                        textSize = if (isTablet) 18.sp else 16.sp,
                        subtextSize = if (isTablet) 14.sp else 12.sp
                    )

                    ReworkQuickActionButton(
                        icon = Icons.Default.Settings,
                        title = "Settings",
                        subtitle = "Configure API and preferences",
                        onClick = { navController.navigate("settings") },
                        height = quickActionButtonHeight,
                        iconSize = if (isTablet) 32.dp else 24.dp,
                        textSize = if (isTablet) 18.sp else 16.sp,
                        subtextSize = if (isTablet) 14.sp else 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(if (isTablet) 40.dp else 20.dp))
    }
}

@Composable
private fun ReworkQuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    height: androidx.compose.ui.unit.Dp = 72.dp,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    textSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    subtextSize: androidx.compose.ui.unit.TextUnit = 12.sp
) {
    AnimatedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = textSize
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    fontSize = subtextSize
                )
            }
        }
    }
}