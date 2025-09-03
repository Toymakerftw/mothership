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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.example.mothership.demo.DemoKeyManager
import com.example.mothership.ui.SplashScreen
import com.example.mothership.ui.theme.MothershipTheme
import kotlinx.coroutines.launch

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
        // Handle the splash screen transition
        val splashScreen = installSplashScreen()
        
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
        
        // Set up back navigation handling
        setupBackNavigation()
        
        // Register device in background for demo key system
        registerDeviceIfNeeded()
        
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
    
    /**
     * Registers the device with the backend if not already registered
     */
    private fun registerDeviceIfNeeded() {
        lifecycleScope.launch {
            val demoKeyManager = DemoKeyManager(this@MainActivity)
            // Only register if not already registered
            if (demoKeyManager.getDeviceId() == null) {
                demoKeyManager.registerDevice()
            }
        }
    }
    
    // Modern back navigation implementation
    private fun setupBackNavigation() {
        // Handle back navigation based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) uses the new predictive back gesture API
            // This is handled via the manifest attribute android:enableOnBackInvokedCallback="true"
            // No additional code needed here for basic implementation
        } else {
            // For older versions, we use the modern approach with OnBackPressedCallback
            onBackPressedDispatcher.addCallback(
                this,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        handleBackNavigation()
                    }
                }
            )
        }
    }
    
    // Common back navigation handling logic
    private fun handleBackNavigation() {
        // Add your custom back navigation logic here
        // For now, we'll just finish the activity
        finish()
    }
    
    // Device-specific compatibility handling
    private fun isOppoOrOnePlusDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("oppo") || manufacturer.contains("oneplus") || 
               manufacturer.contains("realme") || // Realme is a sub-brand of Oppo
               Build.BRAND.lowercase().contains("oppo") || 
               Build.BRAND.lowercase().contains("oneplus")
    }
    
    // Safe method to perform operations that might not work on Oppo/OnePlus devices
    private fun performSafeOperation(operation: () -> Unit) {
        if (isOppoOrOnePlusDevice()) {
            try {
                // Attempt the operation but catch any exceptions
                operation()
            } catch (e: Exception) {
                Log.e("MainActivity", "Operation failed on Oppo/OnePlus device: ${e.message}")
                // Gracefully handle the failure with an alternative approach
                // For example, show a warning to the user or use a fallback method
            }
        } else {
            // For non-Oppo/OnePlus devices, perform the operation normally
            operation()
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