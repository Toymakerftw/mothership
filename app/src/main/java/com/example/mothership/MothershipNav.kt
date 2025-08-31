
package com.example.mothership

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mothership.ui.MainScreen
import com.example.mothership.ui.AppListScreen
import com.example.mothership.ui.SettingsScreen

@Composable
fun MothershipNav(mainViewModel: MainViewModel, settingsViewModel: SettingsViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main") {
        composable("main") { MainScreen(navController = navController, mainViewModel = mainViewModel) }
        composable("appList") { AppListScreen(navController = navController, mainViewModel = mainViewModel) }
        composable("settings") { SettingsScreen(navController = navController, settingsViewModel = settingsViewModel) }
    }
}
