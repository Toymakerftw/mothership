package com.example.mothership

import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.mothership.ui.theme.LocalThemeManager
import com.example.mothership.ui.theme.MothershipTheme
import com.example.mothership.ui.theme.ThemeManager
import java.io.File

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create Mothership directory and subdirectories
        createMothershipDirectories()

        // Create ThemeManager
        val themeManager = ThemeManager(this)

        val api = (application as MothershipApp).api
        val viewModelFactory = MainViewModelFactory(api, this)
        val viewModel = ViewModelProvider(this, viewModelFactory).get(MainViewModel::class.java)

        setContent {
            CompositionLocalProvider(LocalThemeManager provides themeManager) {
                MothershipTheme(themeManager = themeManager) {
                    // A surface container using the 'background' color from the theme
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        MothershipNav(viewModel)
                    }
                }
            }
        }
    }
    
    private fun createMothershipDirectories() {
        try {
            // Create Mothership directory in internal storage
            val mothershipDir = File(getExternalFilesDir(null)?.parentFile, "Mothership")
            if (!mothershipDir.exists()) {
                mothershipDir.mkdirs()
                Log.d(TAG, "Created Mothership directory: ${mothershipDir.absolutePath}")
            }
            
            // Create subdirectories
            val subDirs = listOf("PWAs", "Templates", "Configs", "Logs")
            for (subDirName in subDirs) {
                val subDir = File(mothershipDir, subDirName)
                if (!subDir.exists()) {
                    subDir.mkdirs()
                    Log.d(TAG, "Created subdirectory: ${subDir.absolutePath}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Mothership directories", e)
        }
    }
}