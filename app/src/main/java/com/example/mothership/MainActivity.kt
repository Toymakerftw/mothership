package com.example.mothership

import android.os.Bundle
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import com.example.mothership.ui.SplashScreen
import com.example.mothership.ui.theme.MothershipTheme

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels { ViewModelFactory(this) }
    private val settingsViewModel: SettingsViewModel by viewModels { ViewModelFactory(this) }
    
    private val generationCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val pwaName = intent.getStringExtra(PwaGenerationService.EXTRA_PWA_NAME) ?: "PWA"
            val success = intent.getBooleanExtra(PwaGenerationService.EXTRA_SUCCESS, false)
            val errorMessage = intent.getStringExtra(PwaGenerationService.EXTRA_ERROR_MESSAGE)
            
            if (success) {
                Toast.makeText(this@MainActivity, "$pwaName has been generated successfully!", Toast.LENGTH_LONG).show()
                // Refresh the PWA list
                mainViewModel.getPwas()
            } else {
                Toast.makeText(this@MainActivity, "Failed to generate $pwaName: ${errorMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private val requestNotificationPermission = 
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Permission granted
            } else {
                // Permission denied
                Toast.makeText(this, "Notification permission is required to show generation status", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Register receiver for generation completion
        val filter = IntentFilter(PwaGenerationService.ACTION_GENERATION_COMPLETE)
        // Using context.registerReceiver with RECEIVER_NOT_EXPORTED flag
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(this, generationCompleteReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(generationCompleteReceiver, filter)
        }
        
        setContent {
            MothershipTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MothershipApp(mainViewModel, settingsViewModel)
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Unregister receiver
        unregisterReceiver(generationCompleteReceiver)
    }
}

@Composable
fun MothershipApp(mainViewModel: MainViewModel, settingsViewModel: SettingsViewModel) {
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onTimeout = { showSplash = false })
    } else {
        MothershipNav(mainViewModel, settingsViewModel)
    }
}