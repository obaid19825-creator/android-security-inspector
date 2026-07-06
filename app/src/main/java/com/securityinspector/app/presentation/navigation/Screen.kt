package com.securityinspector.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object FileAnalysis : Screen("file_analysis")
    data object Report : Screen("report/{scanId}") {
        fun createRoute(scanId: Long) = "report/$scanId"
    }
    data object History : Screen("history")
}
