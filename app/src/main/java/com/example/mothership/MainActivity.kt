package com.example.mothership

import android.os.Bundle
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
import com.example.mothership.ui.SplashScreen
import com.example.mothership.ui.theme.MothershipTheme

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels { ViewModelFactory(this) }
    private val settingsViewModel: SettingsViewModel by viewModels { ViewModelFactory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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