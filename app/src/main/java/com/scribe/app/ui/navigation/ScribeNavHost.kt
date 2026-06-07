package com.scribe.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.scribe.app.data.repository.AuthState
import com.scribe.app.ui.common.SplashScreen
import com.scribe.app.ui.home.HomeScreen
import com.scribe.app.ui.incidents.CreateIncidentScreen
import com.scribe.app.ui.incidents.IncidentDetailScreen
import com.scribe.app.ui.login.LoginScreen

private object Routes {
    const val LOADING = "loading"
    const val LOGIN = "login"
    const val HOME = "home"
    const val CREATE = "create_incident"
}

@Composable
fun ScribeNavHost(rootViewModel: RootViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val auth by rootViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(auth) {
        when (auth) {
            is AuthState.LoggedIn ->
                navController.navigate(Routes.HOME) {
                    popUpTo(0) { inclusive = true }
                }
            is AuthState.LoggedOut ->
                navController.navigate(Routes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            is AuthState.Loading -> Unit
        }
    }

    NavHost(navController = navController, startDestination = Routes.LOADING) {
        composable(Routes.LOADING) {
            SplashScreen()
        }
        composable(Routes.LOGIN) {
            LoginScreen()
        }
        composable(Routes.HOME) {
            HomeScreen(
                onOpenDetail = { id -> navController.navigate("incident/$id") },
                onCreateIncident = { navController.navigate(Routes.CREATE) },
                onLogout = { rootViewModel.logout() },
            )
        }
        composable(
            route = "incident/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) {
            IncidentDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CREATE) {
            CreateIncidentScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
