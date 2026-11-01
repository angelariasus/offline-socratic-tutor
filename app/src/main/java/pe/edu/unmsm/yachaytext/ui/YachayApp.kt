package pe.edu.unmsm.yachaytext.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pe.edu.unmsm.yachaytext.ui.biblioteca.BibliotecaRoute
import pe.edu.unmsm.yachaytext.ui.chat.ChatRoute
import pe.edu.unmsm.yachaytext.ui.chat.ChatViewModel
import pe.edu.unmsm.yachaytext.ui.escaneo.EscaneoPendienteScreen
import pe.edu.unmsm.yachaytext.ui.inicio.InicioRoute
import pe.edu.unmsm.yachaytext.ui.theme.Mist
import pe.edu.unmsm.yachaytext.ui.theme.Paper
import pe.edu.unmsm.yachaytext.ui.tutor.TutorRoute

/** Pestañas principales (Figura 3.8 del documento de avance). */
enum class TopLevelDestination(val route: String, val label: String, val icon: ImageVector) {
    Inicio("inicio", "Inicio", Icons.Outlined.Home),
    Biblioteca("biblioteca", "Biblioteca", Icons.Outlined.LocalLibrary),
    Tutor("tutor", "Tutor", Icons.Outlined.Forum),
    Ajustes("ajustes", "Ajustes", Icons.Outlined.Settings),
}

private const val RUTA_ESCANEO = "escaneo"
private const val RUTA_CHAT = "chat/{${ChatViewModel.ARG_CONVERSACION}}"
private fun rutaChat(id: Long) = "chat/$id"

@Composable
fun YachayApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val enPestana = TopLevelDestination.entries.any { it.route == currentRoute }

    // Cada pantalla gestiona sus propios márgenes de sistema (barra de estado, teclado).
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = { if (enPestana) BarraInferior(navController, currentRoute) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Inicio.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(TopLevelDestination.Inicio.route) {
                InicioRoute(onAbrirChat = { navController.navigate(rutaChat(it)) }, onEscanear = { navController.navigate(RUTA_ESCANEO) })
            }
            composable(TopLevelDestination.Biblioteca.route) {
                BibliotecaRoute(onEscanear = { navController.navigate(RUTA_ESCANEO) }, onAbrirChat = { navController.navigate(rutaChat(it)) })
            }
            composable(RUTA_ESCANEO) { EscaneoPendienteScreen(onVolver = { navController.popBackStack() }) }
            composable(TopLevelDestination.Tutor.route) {
                TutorRoute(onAbrirChat = { navController.navigate(rutaChat(it)) })
            }
            composable(TopLevelDestination.Ajustes.route) { PlaceholderScreen("Ajustes") }
            composable(
                RUTA_CHAT,
                arguments = listOf(navArgument(ChatViewModel.ARG_CONVERSACION) { type = NavType.LongType }),
            ) {
                ChatRoute(onVolver = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun BarraInferior(navController: NavHostController, currentRoute: String?) {
    NavigationBar(containerColor = Mist) {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Paper,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(Modifier.fillMaxSize().background(Paper).statusBarsPadding(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
    }
}
