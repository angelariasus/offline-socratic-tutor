package pe.edu.unmsm.yachaytext.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
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
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pe.edu.unmsm.yachaytext.ui.ajustes.AjustesRoute
import pe.edu.unmsm.yachaytext.ui.biblioteca.BibliotecaRoute
import pe.edu.unmsm.yachaytext.ui.bienvenida.BienvenidaRoute
import pe.edu.unmsm.yachaytext.ui.chat.ChatRoute
import pe.edu.unmsm.yachaytext.ui.chat.ChatViewModel
import pe.edu.unmsm.yachaytext.ui.escaneo.EscaneoRoute
import pe.edu.unmsm.yachaytext.ui.escaneo.RevisionRoute
import pe.edu.unmsm.yachaytext.ui.inicio.InicioRoute
import pe.edu.unmsm.yachaytext.ui.libro.LibroRoute
import pe.edu.unmsm.yachaytext.ui.libro.LibroViewModel
import pe.edu.unmsm.yachaytext.ui.theme.Mist
import pe.edu.unmsm.yachaytext.ui.theme.Paper
import pe.edu.unmsm.yachaytext.ui.tutor.TutorRoute

/** Pestañas principales (Figura 3.8 del documento de avance). */
enum class TopLevelDestination(val route: String, val label: String, val icon: ImageVector) {
    Inicio("inicio", "Inicio", Icons.Outlined.Home),
    Biblioteca("biblioteca", "Biblioteca", Icons.AutoMirrored.Outlined.LibraryBooks),
    Tutor("tutor", "Tutor", Icons.Outlined.Forum),
    Ajustes("ajustes", "Ajustes", Icons.Outlined.Settings),
}

const val RUTA_BIENVENIDA = "bienvenida"
private const val RUTA_ESCANEO = "escaneo"
private const val RUTA_REVISION = "revision"
private const val RUTA_CHAT = "chat/{${ChatViewModel.ARG_CONVERSACION}}"
private fun rutaChat(id: Long) = "chat/$id"
private const val RUTA_LIBRO = "libro/{${LibroViewModel.ARG_LIBRO}}"
private fun rutaLibro(id: Long) = "libro/$id"

@Composable
fun YachayApp(navController: NavHostController = rememberNavController(), rutaInicial: String = TopLevelDestination.Inicio.route) {
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
            startDestination = rutaInicial,
            modifier = Modifier.padding(padding),
        ) {
            composable(TopLevelDestination.Inicio.route) {
                InicioRoute(
                    onAbrirChat = { navController.navigate(rutaChat(it)) },
                    onAbrirLibro = { navController.navigate(rutaLibro(it)) },
                    onEscanear = { navController.navigate(RUTA_ESCANEO) },
                    onVerBiblioteca = { irAPestana(navController, TopLevelDestination.Biblioteca.route) },
                )
            }
            composable(TopLevelDestination.Biblioteca.route) {
                BibliotecaRoute(onEscanear = { navController.navigate(RUTA_ESCANEO) }, onAbrirLibro = { navController.navigate(rutaLibro(it)) })
            }
            composable(RUTA_LIBRO, arguments = listOf(navArgument(LibroViewModel.ARG_LIBRO) { type = NavType.LongType })) {
                LibroRoute(
                    onVolver = { navController.popBackStack() },
                    onAbrirChat = { navController.navigate(rutaChat(it)) },
                    onEscanear = { navController.navigate(RUTA_ESCANEO) },
                )
            }
            composable(RUTA_ESCANEO) {
                EscaneoRoute(onVolver = { navController.popBackStack() }, onTextoLeido = { navController.navigate(RUTA_REVISION) })
            }
            composable(RUTA_REVISION) {
                RevisionRoute(
                    onVolver = { navController.popBackStack() },
                    onAbrirChat = { id ->
                        navController.navigate(rutaChat(id)) { popUpTo(TopLevelDestination.Inicio.route) }
                    },
                    onEscanearOtra = { navController.popBackStack(RUTA_ESCANEO, inclusive = false) },
                    onIrBiblioteca = { irAPestana(navController, TopLevelDestination.Biblioteca.route) },
                )
            }
            composable(TopLevelDestination.Tutor.route) {
                TutorRoute(onAbrirChat = { navController.navigate(rutaChat(it)) })
            }
            composable(RUTA_BIENVENIDA) {
                BienvenidaRoute(onTerminar = {
                    navController.navigate(TopLevelDestination.Inicio.route) { popUpTo(RUTA_BIENVENIDA) { inclusive = true } }
                })
            }
            composable(TopLevelDestination.Ajustes.route) { AjustesRoute() }
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
                onClick = { irAPestana(navController, destination.route) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Paper,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

private fun irAPestana(navController: NavHostController, ruta: String) {
    navController.navigate(ruta) {
        // Inicio siempre está en la base de la pila (la bienvenida se retira al terminar).
        popUpTo(TopLevelDestination.Inicio.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
