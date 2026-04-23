package com.somnil.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.somnil.app.ui.screens.history.HistoryView
import com.somnil.app.ui.screens.home.HomeView
import com.somnil.app.ui.screens.live.LiveMonitorView
import com.somnil.app.ui.screens.settings.SettingsView
import com.somnil.app.ui.screens.training.TrainingView
import com.somnil.app.ui.theme.*

/**
 * Main navigation routes.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen("home", "首页", Icons.Filled.Home, Icons.Outlined.Home)
    data object LiveMonitor : Screen("live", "监测", Icons.Filled.ShowChart, Icons.Outlined.ShowChart)
    data object History : Screen("history", "历史", Icons.Filled.History, Icons.Outlined.History)
    data object Training : Screen("training", "训练", Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp)
    data object Settings : Screen("settings", "设置", Icons.Filled.Settings, Icons.Outlined.Settings)
}

private val bottomNavItems = listOf(
    Screen.Home,
    Screen.LiveMonitor,
    Screen.Training,
    Screen.History,
    Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SomnilApp() {
    val navController = rememberNavController()

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = {
            NavigationBar(
                containerColor = BackgroundMid,
                contentColor = TextPrimary
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentPurple,
                            selectedTextColor = AccentPurple,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = CardBackground
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeView(
                    onNavigateToLiveMonitor = {
                        navController.navigate(Screen.LiveMonitor.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            composable(Screen.LiveMonitor.route) {
                LiveMonitorView()
            }

            composable(Screen.History.route) {
                HistoryView()
            }

            composable(Screen.Training.route) {
                TrainingView()
            }

            composable(Screen.Settings.route) {
                SettingsView()
            }
        }
    }
}