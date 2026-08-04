package com.rahul.stocksim.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Help
import androidx.compose.ui.graphics.vector.ImageVector

// Shared constant to avoid duplication and conflicts
const val WEB_CLIENT_ID = "921964890596-iqltc99aa0dbc73p644csaa5p8qcmeph.apps.googleusercontent.com"

sealed class Screen(val route: String) {
    object Login : Screen("login_screen?error={error}") {
        fun createRoute(error: String? = null): String {
            return if (error != null) "login_screen?error=$error" else "login_screen"
        }
    }
    object Register : Screen("register_screen")
    object PasswordSetup : Screen("password_setup_screen/{isChangePassword}?name={name}&email={email}&wantsPro={wantsPro}") {
        fun createRoute(isChangePassword: Boolean, name: String? = null, email: String? = null, wantsPro: Boolean = false): String {
            val base = "password_setup_screen/$isChangePassword"
            val queryParams = mutableListOf<String>()
            if (name != null) queryParams.add("name=$name")
            if (email != null) queryParams.add("email=$email")
            queryParams.add("wantsPro=$wantsPro")
            return if (queryParams.isEmpty()) base else "$base?${queryParams.joinToString("&")}"
        }
    }
    object BalanceSelection : Screen("balance_selection_screen?name={name}&email={email}&password={password}&wantsPro={wantsPro}") {
        fun createRoute(name: String? = null, email: String? = null, password: String? = null, wantsPro: Boolean = false): String {
            val base = "balance_selection_screen"
            val queryParams = mutableListOf<String>()
            if (name != null) queryParams.add("name=$name")
            if (email != null) queryParams.add("email=$email")
            if (password != null) queryParams.add("password=$password")
            queryParams.add("wantsPro=$wantsPro")
            return if (queryParams.isEmpty()) base else "$base?${queryParams.joinToString("&")}"
        }
    }
    object Settings : Screen("settings_screen")
    object Upgrade : Screen("upgrade_screen")
    object EditProfile : Screen("edit_profile_screen")
    object Main : Screen("main_screen")
    object Details : Screen("details/{symbol}") {
        fun createRoute(symbol: String) = "details/$symbol"
    }
    object Achievements : Screen("achievements")
    object MarketTutorial : Screen("market_tutorial")
    object CreatePortfolio : Screen("create_portfolio_screen")
}

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Portfolio : BottomNavItem("portfolio_screen", "Portfolio", Icons.Default.AccountBalanceWallet)
    object Market : BottomNavItem("market_screen", "Market", Icons.Default.BarChart)
    object Contracts : BottomNavItem("contracts_screen", "Contracts", Icons.Default.AttachMoney)
    object Leaderboard : BottomNavItem("leaderboard_screen", "Leaders", Icons.Default.EmojiEvents)
    object Insights : BottomNavItem("insights_screen", "Insights", Icons.AutoMirrored.Filled.TrendingUp)
    object Guide : BottomNavItem("guide_screen", "Guide", Icons.Default.Help)
}
