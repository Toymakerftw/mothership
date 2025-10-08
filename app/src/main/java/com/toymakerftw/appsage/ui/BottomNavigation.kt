package com.toymakerftw.appsage.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun AppsageBottomNavigation(navController: NavController?) {
    if (navController == null) return
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        BottomNavItem.items.forEach { item ->
            val isSelected = currentRoute == item.route
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.9f else 1f,
                animationSpec = tween(100, easing = FastOutSlowInEasing),
                label = "scale"
            )
            
            val iconScale by animateFloatAsState(
                targetValue = if (isSelected) 1.2f else 1f,
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                label = "icon_scale"
            )
            
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.scale(iconScale)
                    )
                },
                label = { 
                    AnimatedVisibility(
                        visible = isSelected,
                        enter = expandVertically(
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeIn(
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ),
                        exit = shrinkVertically(
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeOut(
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        )
                    ) {
                        Text(item.label)
                    }
                },
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                },
                interactionSource = interactionSource,
                modifier = Modifier.scale(scale),
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Home : BottomNavItem("main", Icons.Default.Home, "Home")
    object AppList : BottomNavItem("app_list", Icons.Default.Apps, "My Apps")
    object Settings : BottomNavItem("settings", Icons.Default.Settings, "Settings")
    
    companion object {
        val items = listOf(Home, AppList, Settings)
    }
}