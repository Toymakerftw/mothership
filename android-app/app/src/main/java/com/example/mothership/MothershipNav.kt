package com.example.mothership

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mothership.ui.AppListScreen
import com.example.mothership.ui.MainScreen
import com.example.mothership.ui.SettingsScreen
import com.example.mothership.ui.theme.LocalThemeManager

sealed class Screen(val route: String, val title: String, val icon: @Composable () -> Unit) {
    object Home : Screen("main", "Home", { Icon(Icons.Filled.Home, contentDescription = null) })
    object Apps : Screen("apps", "Apps", { Icon(Icons.Filled.List, contentDescription = null) })
    object Settings : Screen("settings", "Settings", { Icon(Icons.Filled.Settings, contentDescription = null) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MothershipNav(viewModel: MainViewModel) {
    val navController = rememberNavController()
    var selectedItem by rememberSaveable { mutableStateOf(0) }
    val themeManager = LocalThemeManager.current
    
    val items = listOf(
        Screen.Home,
        Screen.Apps,
        Screen.Settings
    )
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = item.icon,
                        label = { Text(item.title) },
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                            navController.navigate(item.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = androidx.compose.ui.Modifier.padding(innerPadding)) {
            NavigationHost(navController, viewModel, themeManager)
        }
    }
}

@Composable
fun NavigationHost(navController: NavHostController, viewModel: MainViewModel, themeManager: com.example.mothership.ui.theme.ThemeManager) {
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                viewModel = viewModel,
                onNavigateToApps = { navController.navigate("apps") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("apps") {
            AppListScreen(
                onBackClicked = { navController.popBackStack() },
                onSettingsClicked = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            val settingsViewModel: SettingsViewModel = viewModel()
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
