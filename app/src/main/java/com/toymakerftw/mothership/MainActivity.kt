package com.toymakerftw.mothership

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
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.toymakerftw.mothership.demo.DemoKeyManager
import com.toymakerftw.mothership.ui.SplashScreen
import com.toymakerftw.mothership.ui.theme.MothershipTheme
import kotlinx.coroutines.launch
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.toymakerftw.mothership.work.PwaWorkManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels { ViewModelFactory(this) }
    private val settingsViewModel: SettingsViewModel by viewModels { ViewModelFactory(this) }
    
    private val generationCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val pwaName = intent.getStringExtra(PwaGenerationService.EXTRA_PWA_NAME) ?: "PWA"
            val success = intent.getBooleanExtra(PwaGenerationService.EXTRA_SUCCESS, false)
            val errorMessage = intent.getStringExtra(PwaGenerationService.EXTRA_ERROR_MESSAGE)
            
            // Only show toast and update UI if app is in foreground
            if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                if (success) {
                    Toast.makeText(this@MainActivity, "$pwaName has been generated successfully!", Toast.LENGTH_LONG).show()
                    // Refresh the PWA list
                    mainViewModel.getPwas()
                } else {
                    Toast.makeText(this@MainActivity, "Failed to generate $pwaName: ${errorMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                }
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
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Handle navigation from notifications
        handleNotificationNavigation(intent)
        
        // Add lifecycle observer to handle app lifecycle events
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                // App is going to background, pause any ongoing operations if needed
                Log.d("MainActivity", "App is going to background")
            }
            
            override fun onResume(owner: LifecycleOwner) {
                // App is coming to foreground, resume operations if needed
                Log.d("MainActivity", "App is coming to foreground")
            }
        })
        
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
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle navigation from notifications when app is already running
        intent?.let { handleNotificationNavigation(it) }
    }
    
    private fun handleNotificationNavigation(intent: Intent) {
        val navigateTo = intent.getStringExtra("navigate_to")
        if (navigateTo == "appList") {
            // We'll handle this in the Compose navigation
            Log.d("MainActivity", "Received navigation request to appList")
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
    
    override fun onResume() {
        super.onResume()
        // Refresh PWA list when app comes to foreground
        mainViewModel.getPwas()
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