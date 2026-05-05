package com.rdsocial.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rdsocial.presentation.auth.AuthNavHost
import com.rdsocial.presentation.home.HomeScreen
import com.rdsocial.presentation.navigation.NavRoutes
import com.rdsocial.presentation.profile.ProfileScreen

@Composable
fun RDSocialApp(
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val authUser by appViewModel.authUser.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        if (authUser != null) {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = NavRoutes.Home,
            ) {
                composable(NavRoutes.Home) {
                    HomeScreen(
                        onNavigateToProfile = { navController.navigate(NavRoutes.Profile) },
                    )
                }
                composable(NavRoutes.Profile) {
                    ProfileScreen(
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
            }
        } else {
            AuthNavHost()
        }
    }
}
