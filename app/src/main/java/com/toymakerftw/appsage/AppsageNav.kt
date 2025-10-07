package com.toymakerftw.appsage

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.toymakerftw.appsage.ui.MainScreen
import com.toymakerftw.appsage.ui.AppListScreen
import com.toymakerftw.appsage.ui.ReworkScreen
import com.toymakerftw.appsage.ui.AppsageBottomNavigation
import com.toymakerftw.appsage.ReworkViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppsageNav() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    
    // Don't show bottom navigation on rework screen
    val showBottomNav = navBackStackEntry.value?.destination?.route?.startsWith("rework") == false
    
    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                AppsageBottomNavigation(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "main",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("main") {
                val mainViewModel: MainViewModel = viewModel { 
                    val settingsRepository = com.toymakerftw.appsage.data.SettingsRepository(context)
                    val appsageApi = (context.applicationContext as AppsageApp).appsageApi
                    MainViewModel(context, appsageApi, settingsRepository)
                }
                MainScreen(mainViewModel, navController)
            }
            composable("pwa_list") {
                val mainViewModel: MainViewModel = viewModel { 
                    val settingsRepository = com.toymakerftw.appsage.data.SettingsRepository(context)
                    val appsageApi = (context.applicationContext as AppsageApp).appsageApi
                    MainViewModel(context, appsageApi, settingsRepository)
                }
                AppListScreen(navController, mainViewModel)
            }
            composable("rework/{uuid}") { backStackEntry ->
                val uuid = backStackEntry.arguments?.getString("uuid") ?: ""
                val reworkViewModel: ReworkViewModel = viewModel {
                    val settingsRepository = com.toymakerftw.appsage.data.SettingsRepository(context)
                    val appsageApi = (context.applicationContext as AppsageApp).appsageApi
                    ReworkViewModel(context, appsageApi, settingsRepository)
                }
                ReworkScreen(uuid, reworkViewModel)
            }
        }
    }
}