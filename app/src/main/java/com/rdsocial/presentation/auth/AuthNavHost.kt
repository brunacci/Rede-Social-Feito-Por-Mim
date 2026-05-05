package com.rdsocial.presentation.auth

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rdsocial.presentation.navigation.NavRoutes

@Composable
fun AuthNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.Login,
    ) {
        composable(NavRoutes.Login) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(NavRoutes.Register) },
            )
        }
        composable(NavRoutes.Register) {
            RegisterScreen(
                onNavigateBackToLogin = { navController.popBackStack() },
            )
        }
    }
}
