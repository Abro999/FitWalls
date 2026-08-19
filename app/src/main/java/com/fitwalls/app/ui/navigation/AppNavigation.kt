package com.fitwalls.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fitwalls.app.ui.screens.home.HomeScreen
import com.fitwalls.app.ui.screens.login.LoginScreen
import com.fitwalls.app.ui.screens.generator.GeneratorScreen
import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

@Serializable
object HomeRoute

@Serializable
object GeneratorRoute

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = LoginRoute) {
        composable<LoginRoute> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(HomeRoute) {
                        popUpTo(LoginRoute) { inclusive = true }
                    }
                }
            )
        }
        
        composable<HomeRoute> {
            HomeScreen(
                onNavigateToGenerator = {
                    navController.navigate(GeneratorRoute)
                }
            )
        }
        
        composable<GeneratorRoute> {
            GeneratorScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
