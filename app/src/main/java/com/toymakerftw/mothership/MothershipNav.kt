package com.toymakerftw.mothership

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
import androidx.navigation.navArgument
import com.toymakerftw.mothership.ui.AppListScreen
import com.toymakerftw.mothership.ui.BottomNavigationBar
import com.toymakerftw.mothership.ui.MainScreen
import com.toymakerftw.mothership.ui.ReworkScreen
import com.toymakerftw.mothership.ui.SettingsScreen

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
        composable(
            "rework/{pwaUuid}/{pwaName}",
            arguments = listOf(
                navArgument("pwaUuid") { defaultValue = "" },
                navArgument("pwaName") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val pwaUuid = backStackEntry.arguments?.getString("pwaUuid") ?: ""
            val pwaName = backStackEntry.arguments?.getString("pwaName") ?: ""
            ReworkScreen(
                navController = navController,
                mainViewModel = mainViewModel,
                pwaUuid = pwaUuid,
                pwaName = pwaName
            )
        }
        composable("settings") { SettingsScreen(navController = navController, settingsViewModel = settingsViewModel) }
    }
}