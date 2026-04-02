package com.toymakerftw.appsage.ui

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.toymakerftw.appsage.ui.theme.AppsageTheme
import com.toymakerftw.appsage.widget.DynamicGlanceWidget
import com.toymakerftw.appsage.widget.WidgetGenerationWorker
import com.toymakerftw.appsage.widget.WidgetManager
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val widgetManager = WidgetManager(this)

        setContent {
            AppsageTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WidgetConfigScreen(
                        widgetManager = widgetManager,
                        onSelectExisting = { storedWidget ->
                            applyStoredWidgetAndFinish(storedWidget)
                        },
                        onGenerateNew = { prompt ->
                            startGenerationAndFinish(prompt)
                        }
                    )
                }
            }
        }
    }

    private fun applyStoredWidgetAndFinish(stored: WidgetManager.StoredWidget) {
        val context = this
        val scope = kotlinx.coroutines.MainScope()
        scope.launch {
            try {
                val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs[DynamicGlanceWidget.KEY_LAYOUT_JSON] = stored.layoutJson
                    prefs[DynamicGlanceWidget.KEY_PROMPT] = stored.prompt
                }
                DynamicGlanceWidget().update(context, glanceId)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }

    private fun startGenerationAndFinish(prompt: String) {
        val workRequest = OneTimeWorkRequestBuilder<WidgetGenerationWorker>()
            .setInputData(
                Data.Builder()
                    .putString(WidgetGenerationWorker.KEY_PROMPT, prompt)
                    .putInt(WidgetGenerationWorker.KEY_APP_WIDGET_ID, appWidgetId)
                    .build()
            )
            .build()
            
        WorkManager.getInstance(this).enqueue(workRequest)

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    widgetManager: WidgetManager,
    onSelectExisting: (WidgetManager.StoredWidget) -> Unit,
    onGenerateNew: (String) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var showNewWidgetForm by remember { mutableStateOf(false) }
    val storedWidgets = remember { widgetManager.getGeneratedWidgets() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Select Widget",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${storedWidgets.size} widget${if (storedWidgets.size != 1) "s" else ""} available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Existing widgets
            if (storedWidgets.isNotEmpty()) {
                item {
                    Text(
                        text = "Your Widgets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                
                items(storedWidgets) { widget ->
                    StoredWidgetCard(
                        widget = widget,
                        onClick = { onSelectExisting(widget) }
                    )
                }
                
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            // Create New section
            item {
                Text(
                    text = "Create New",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            item {
                AnimatedVisibility(
                    visible = !showNewWidgetForm,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNewWidgetForm = true },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Generate a new widget with AI",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = showNewWidgetForm,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            OutlinedTextField(
                                value = prompt,
                                onValueChange = { prompt = it },
                                label = { Text("Describe your widget") },
                                placeholder = { Text("e.g., A motivational quote with a refresh button") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showNewWidgetForm = false },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = { onGenerateNew(prompt) },
                                    modifier = Modifier.weight(1f),
                                    enabled = prompt.isNotBlank(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generate")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoredWidgetCard(
    widget: WidgetManager.StoredWidget,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Widgets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = widget.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = widget.prompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Apply",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
