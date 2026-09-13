package com.fitwalls.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.fitwalls.app.ui.screens.home.HomeScreen
import com.fitwalls.app.ui.screens.login.LoginScreen
import com.fitwalls.app.ui.screens.generator.GeneratorScreen
import com.fitwalls.app.ui.screens.upload.UploadScreen
import com.fitwalls.app.ui.screens.account.AccountScreen
import com.fitwalls.app.ui.screens.upload.EditWallpaperScreen
import kotlinx.serialization.Serializable

import com.fitwalls.app.ui.screens.auth.RoleSelectionScreen
import androidx.compose.runtime.rememberCoroutineScope
import com.fitwalls.app.data.FirestoreManager
import kotlinx.coroutines.launch

@Serializable
object LoginRoute

@Serializable
object RoleSelectionRoute

@Serializable
object HomeRoute

@Serializable
object GeneratorRoute

@Serializable
object UploadRoute

@Serializable
object AccountRoute

@Serializable
data class EditWallpaperRoute(val wallpaperId: String)

@Serializable
data class PreviewRoute(val wallpaperId: String)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val firestoreManager = remember { FirestoreManager() }
    
    NavHost(navController = navController, startDestination = LoginRoute) {
        composable<LoginRoute> {
            LoginScreen(
                onLoginSuccess = {
                    scope.launch {
                        val role = firestoreManager.getUserRole()
                        if (role == null) {
                            navController.navigate(RoleSelectionRoute) {
                                popUpTo(LoginRoute) { inclusive = true }
                            }
                        } else {
                            navController.navigate(HomeRoute) {
                                popUpTo(LoginRoute) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }
        
        composable<RoleSelectionRoute> {
            RoleSelectionScreen(
                onRoleSelected = {
                    navController.navigate(HomeRoute) {
                        popUpTo(RoleSelectionRoute) { inclusive = true }
                    }
                }
            )
        }
        
        composable<HomeRoute> {
            HomeScreen(
                onNavigateToGenerator = {
                    navController.navigate(GeneratorRoute)
                },
                onNavigateToUpload = {
                    navController.navigate(UploadRoute)
                },
                onNavigateToPreview = { wallpaperId ->
                    navController.navigate(PreviewRoute(wallpaperId))
                },
                onNavigateToAccount = {
                    navController.navigate(AccountRoute)
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
        
        composable<UploadRoute> {
            UploadScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<AccountRoute> {
            AccountScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSignOut = {
                    navController.navigate(LoginRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToUpload = {
                    navController.navigate(UploadRoute)
                },
                onNavigateToEditWallpaper = { wallpaperId ->
                    navController.navigate(EditWallpaperRoute(wallpaperId))
                }
            )
        }

        composable<EditWallpaperRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<EditWallpaperRoute>()
            EditWallpaperScreen(
                wallpaperId = route.wallpaperId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<PreviewRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PreviewRoute>()
            com.fitwalls.app.ui.screens.preview.PreviewScreen(
                wallpaperId = route.wallpaperId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
