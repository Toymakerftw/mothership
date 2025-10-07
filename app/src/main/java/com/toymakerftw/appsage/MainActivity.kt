package com.toymakerftw.appsage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.toymakerftw.appsage.api.AppsageApi
import com.toymakerftw.appsage.data.SettingsRepository
import com.toymakerftw.appsage.ui.theme.AppsageTheme

class MainActivity : ComponentActivity() {

    private val appsageApi: AppsageApi by lazy { 
        (application as AppsageApp).appsageApi 
    }
    
    private val settingsRepository: SettingsRepository by lazy { 
        SettingsRepository(this) 
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppsageTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppsageNav()
                }
            }
        }
    }
}