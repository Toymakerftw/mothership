package com.example.mothership

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mothership.ui.AppListScreen
import com.example.mothership.ui.BottomNavigationBar
import com.example.mothership.ui.MainScreen
import com.example.mothership.ui.SettingsScreen

@Composable
fun MothershipNav(mainViewModel: MainViewModel, settingsViewModel: SettingsViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "main"

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                currentRoute = currentRoute
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavigationHost(
                navController = navController,
                mainViewModel = mainViewModel,
                settingsViewModel = settingsViewModel
            )
        }
    }
}

@Composable
fun NavigationHost(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel
) {
    NavHost(navController = navController, startDestination = "main") {
        composable("main") { MainScreen(navController = navController, mainViewModel = mainViewModel) }
        composable("appList") { AppListScreen(navController = navController, mainViewModel = mainViewModel) }
        composable("settings") { SettingsScreen(navController = navController, settingsViewModel = settingsViewModel) }
    }
}