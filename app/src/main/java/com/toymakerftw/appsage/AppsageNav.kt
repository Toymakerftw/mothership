package com.toymakerftw.appsage

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toymakerftw.appsage.ui.MainScreen
import com.toymakerftw.appsage.ui.PwaListScreen
import com.toymakerftw.appsage.ui.ReworkScreen
import com.toymakerftw.appsage.ReworkViewModel

@Composable
fun AppsageNav() {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            val mainViewModel: MainViewModel = viewModel { 
                val settingsRepository = com.toymakerftw.appsage.data.SettingsRepository(context)
                val appsageApi = (context.applicationContext as AppsageApp).appsageApi
                MainViewModel(context, appsageApi, settingsRepository)
            }
            MainScreen(mainViewModel)
        }
        composable("pwa_list") {
            PwaListScreen(context)
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