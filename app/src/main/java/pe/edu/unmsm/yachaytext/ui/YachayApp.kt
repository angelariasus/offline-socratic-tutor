package pe.edu.unmsm.yachaytext.ui

import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.edu.unmsm.yachaytext.ui.common.LogoYachay
import pe.edu.unmsm.yachaytext.ui.common.encogerAlPresionar
import pe.edu.unmsm.yachaytext.ui.common.movimientoReducido
import pe.edu.unmsm.yachaytext.ui.theme.Brand
import pe.edu.unmsm.yachaytext.ui.theme.Linen
import pe.edu.unmsm.yachaytext.ui.theme.TextSecondary
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import pe.edu.unmsm.yachaytext.ui.theme.Paper
import pe.edu.unmsm.yachaytext.ui.tutor.TutorRoute

/** Pestañas principales (Figura 3.8 del documento de avance). */
/** Ícono de contorno en reposo y relleno al estar seleccionado (señal de estado más clara que el color). */
enum class TopLevelDestination(val route: String, val label: String, val icon: ImageVector, val iconoActivo: ImageVector) {
    Inicio("inicio", "Inicio", Icons.Outlined.Home, Icons.Filled.Home),
    Biblioteca("biblioteca", "Biblioteca", Icons.AutoMirrored.Outlined.LibraryBooks, Icons.AutoMirrored.Filled.LibraryBooks),
    Tutor("tutor", "Tutor", Icons.Outlined.Forum, Icons.Filled.Forum),
    Ajustes("ajustes", "Ajustes", Icons.Outlined.Settings, Icons.Filled.Settings),
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
    val irA: (String) -> Unit = { irAPestana(navController, it) }

    BoxWithConstraints(Modifier.fillMaxSize().background(Linen)) {
        if (maxWidth >= 840.dp) {
            // Pantalla ancha (tableta en horizontal): la navegación pasa a un riel lateral y libera altura.
            Row(Modifier.fillMaxSize()) {
                if (enPestana) RielNavegacion(currentRoute, irA)
                Destinos(navController, rutaInicial, Modifier.weight(1f))
            }
        } else {
            // Cada pantalla gestiona sus propios márgenes de sistema (barra de estado, teclado).
            Scaffold(
                containerColor = Linen,
                contentWindowInsets = WindowInsets(0),
                bottomBar = { if (enPestana) BarraFlotante(currentRoute, irA) },
            ) { padding ->
                Destinos(navController, rutaInicial, Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun Destinos(navController: NavHostController, rutaInicial: String, modifier: Modifier) {
    NavHost(navController = navController, startDestination = rutaInicial, modifier = modifier) {
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

private val AnchoIndicador = 56.dp

/**
 * Barra inferior flotante: una píldora separada de los bordes. El indicador se desliza con un
 * resorte de una pestaña a otra (sin rebote, interrumpible) y se omite si se quitaron las animaciones.
 */
@Composable
private fun BarraFlotante(actual: String?, onIr: (String) -> Unit) {
    val destinos = TopLevelDestination.entries
    val indice = destinos.indexOfFirst { it.route == actual }.coerceAtLeast(0)
    val reducido = movimientoReducido()
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 6.dp), contentAlignment = Alignment.Center) {
        Surface(color = Paper, shape = RoundedCornerShape(32.dp), shadowElevation = 8.dp, modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().height(72.dp)) {
            BoxWithConstraints {
                val anchoItem = maxWidth / destinos.size
                val x by animateDpAsState(
                    targetValue = anchoItem * indice + (anchoItem - AnchoIndicador) / 2,
                    animationSpec = if (reducido) snap() else spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                    label = "indicador",
                )
                Box(Modifier.offset(x = x, y = 10.dp).size(AnchoIndicador, 32.dp).clip(RoundedCornerShape(50)).background(Brand))
                Row(Modifier.fillMaxSize()) {
                    destinos.forEach { d -> ItemBarra(d, d.route == actual, Modifier.weight(1f)) { onIr(d.route) } }
                }
            }
        }
    }
}

@Composable
private fun ItemBarra(destino: TopLevelDestination, seleccionado: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val interaccion = remember { MutableInteractionSource() }
    val tinte by animateColorAsState(if (seleccionado) Paper else TextSecondary, label = "icono")
    val texto by animateColorAsState(if (seleccionado) Brand else TextSecondary, label = "etiqueta")
    Column(
        modifier
            .fillMaxHeight()
            .selectable(seleccionado, interactionSource = interaccion, indication = null, role = Role.Tab, onClick = onClick)
            .encogerAlPresionar(interaccion),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.padding(top = 10.dp).size(AnchoIndicador, 32.dp), contentAlignment = Alignment.Center) {
            Icon(if (seleccionado) destino.iconoActivo else destino.icon, contentDescription = null, tint = tinte)
        }
        Text(
            destino.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal,
            color = texto,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Riel lateral para pantallas anchas, con el logo arriba y las pestañas centradas. */
@Composable
private fun RielNavegacion(actual: String?, onIr: (String) -> Unit) {
    NavigationRail(containerColor = Paper, header = { LogoYachay(tamano = 48.dp, modifier = Modifier.padding(top = 12.dp)) }) {
        Spacer(Modifier.weight(1f))
        TopLevelDestination.entries.forEach { d ->
            val seleccionado = d.route == actual
            NavigationRailItem(
                selected = seleccionado,
                onClick = { onIr(d.route) },
                icon = { Icon(if (seleccionado) d.iconoActivo else d.icon, contentDescription = null) },
                label = { Text(d.label, fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Paper,
                    selectedTextColor = Brand,
                    indicatorColor = Brand,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                ),
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }
        Spacer(Modifier.weight(1f))
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
