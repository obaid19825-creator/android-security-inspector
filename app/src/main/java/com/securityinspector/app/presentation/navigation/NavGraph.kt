package com.securityinspector.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.securityinspector.app.presentation.dashboard.DashboardScreen
import com.securityinspector.app.presentation.filescan.FileAnalysisScreen
import com.securityinspector.app.presentation.history.HistoryScreen
import com.securityinspector.app.presentation.report.ReportScreen

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "Dashboard", Icons.Filled.Dashboard),
    BottomNavItem(Screen.FileAnalysis, "File Analysis", Icons.Filled.Description),
    BottomNavItem(Screen.History, "History", Icons.Filled.History)
)

@Composable
fun SecurityInspectorNavGraph() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val isTopLevel = bottomNavItems.any { item ->
                currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
            }

            if (isTopLevel) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(onOpenReport = { id -> navController.navigate(Screen.Report.createRoute(id)) })
            }
            composable(Screen.FileAnalysis.route) {
                FileAnalysisScreen()
            }
            composable(Screen.History.route) {
                HistoryScreen(onOpenReport = { id -> navController.navigate(Screen.Report.createRoute(id)) })
            }
            composable(
                route = Screen.Report.route,
                arguments = listOf(navArgument("scanId") { type = NavType.LongType })
            ) { backStackEntry ->
                val scanId = backStackEntry.arguments?.getLong("scanId") ?: -1L
                ReportScreen(scanId = scanId, onBack = { navController.popBackStack() })
            }
        }
    }
}
