package pe.edu.unmsm.yachaytext.ui.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.edu.unmsm.yachaytext.YachayApplication
import pe.edu.unmsm.yachaytext.data.ChatRepository
import pe.edu.unmsm.yachaytext.data.LibroRepository
import pe.edu.unmsm.yachaytext.data.PreferenciasRepository
import pe.edu.unmsm.yachaytext.domain.tutor.IniciarConsulta
import pe.edu.unmsm.yachaytext.ui.biblioteca.LibroUi
import pe.edu.unmsm.yachaytext.ui.common.ConversacionResumen
import pe.edu.unmsm.yachaytext.ui.common.GrupoConversaciones
import pe.edu.unmsm.yachaytext.ui.common.EstadoVacio
import pe.edu.unmsm.yachaytext.ui.common.PortadaLibro
import pe.edu.unmsm.yachaytext.ui.common.TarjetaPortada
import pe.edu.unmsm.yachaytext.ui.common.TituloSeccion
import pe.edu.unmsm.yachaytext.ui.common.colorArea
import pe.edu.unmsm.yachaytext.ui.common.encogerAlPresionar
import pe.edu.unmsm.yachaytext.ui.common.iconoArea
import pe.edu.unmsm.yachaytext.ui.common.resumir
import pe.edu.unmsm.yachaytext.ui.theme.Brand
import pe.edu.unmsm.yachaytext.ui.theme.BrandSoft
import pe.edu.unmsm.yachaytext.ui.theme.Ink
import pe.edu.unmsm.yachaytext.ui.theme.Linen
import pe.edu.unmsm.yachaytext.ui.theme.Paper
import pe.edu.unmsm.yachaytext.ui.theme.TextSecondary

data class InicioUiState(
    val nombre: String? = null,
    val conversaciones: List<ConversacionResumen> = emptyList(),
    val libros: List<LibroUi> = emptyList(),
    val totalLibros: Int = 0,
    val cargando: Boolean = true,
)

private const val RECIENTES = 3
private const val LIBROS_EN_CARRUSEL = 8

class InicioViewModel(
    preferencias: PreferenciasRepository,
    chat: ChatRepository,
    libros: LibroRepository,
    private val iniciarConsulta: IniciarConsulta,
) : ViewModel() {

    val estado: StateFlow<InicioUiState> = combine(preferencias.observar(), chat.observarConversaciones(), libros.observarLibros()) { p, c, l ->
        InicioUiState(
            nombre = p.nombre,
            conversaciones = resumir(c.take(RECIENTES), l),
            libros = l.take(LIBROS_EN_CARRUSEL).map { LibroUi(it.id, it.titulo, it.areaCurso, it.numPaginas, it.fechaIngesta) },
            totalLibros = l.size,
            cargando = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioUiState())

    fun nuevaConsulta(alCrear: (Long) -> Unit) {
        viewModelScope.launch { alCrear(iniciarConsulta()) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                with((this[APPLICATION_KEY] as YachayApplication).container) {
                    InicioViewModel(preferencias, chatRepository, libroRepository, iniciarConsulta)
                }
            }
        }
    }
}

data class AccionesInicio(
    val onPreguntar: () -> Unit = {},
    val onEscanear: () -> Unit = {},
    val onAbrirChat: (Long) -> Unit = {},
    val onAbrirLibro: (Long) -> Unit = {},
    val onVerBiblioteca: () -> Unit = {},
)

@Composable
fun InicioRoute(
    onAbrirChat: (Long) -> Unit,
    onAbrirLibro: (Long) -> Unit,
    onEscanear: () -> Unit,
    onVerBiblioteca: () -> Unit,
    viewModel: InicioViewModel = viewModel(factory = InicioViewModel.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    InicioScreen(
        estado = estado,
        acciones = AccionesInicio(
            onPreguntar = { viewModel.nuevaConsulta(onAbrirChat) },
            onEscanear = onEscanear,
            onAbrirChat = onAbrirChat,
            onAbrirLibro = onAbrirLibro,
            onVerBiblioteca = onVerBiblioteca,
        ),
    )
}

/** Margen lateral de las pantallas; los carruseles lo usan como relleno para llegar al borde. */
private val Margen = 20.dp

@Composable
fun InicioScreen(estado: InicioUiState, acciones: AccionesInicio, ahora: Long = System.currentTimeMillis()) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Linen).statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        val relleno = PaddingValues(vertical = Margen)
        if (maxWidth >= 840.dp) {
            // Pantalla ancha: la portada a todo el ancho y, debajo, conversaciones y libros lado a lado.
            LazyColumn(Modifier.widthIn(max = 1200.dp).fillMaxSize(), contentPadding = relleno) {
                cabecera(estado, acciones, alto = 184.dp)
                item {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) { ColumnaConversaciones(estado, acciones, ahora) }
                        Column(Modifier.weight(1f)) { ColumnaLibros(estado, acciones, ahora) }
                    }
                }
            }
        } else {
            LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize(), contentPadding = relleno) {
                cabecera(estado, acciones)
                conversaciones(estado, acciones, ahora)
                librosEnCarrusel(estado, acciones, ahora)
            }
        }
    }
}

private fun Modifier.margen() = padding(horizontal = Margen)

private fun LazyListScope.cabecera(estado: InicioUiState, acciones: AccionesInicio, alto: Dp = 200.dp) {
    item { Portada(estado.nombre, acciones, alto, Modifier.margen()) }
    if (!estado.cargando && estado.conversaciones.isEmpty() && estado.libros.isEmpty()) {
        item {
            EstadoVacio(
                icono = Icons.AutoMirrored.Outlined.LibraryBooks,
                titulo = "Empieza con tu libro",
                texto = "Escanea una página o hazle una pregunta a Yachay. Todo funciona sin internet.",
                accion = "Escanear página",
                onAccion = acciones.onEscanear,
            )
        }
    }
}

private fun LazyListScope.conversaciones(estado: InicioUiState, acciones: AccionesInicio, ahora: Long) {
    val (destacada, resto) = estado.conversaciones.firstOrNull() to estado.conversaciones.drop(1)
    if (destacada == null) return
    item { TituloSeccion("Continúa donde te quedaste", Modifier.margen().padding(top = 20.dp, bottom = 8.dp)) }
    item { TarjetaContinuar(destacada, ahora, { acciones.onAbrirChat(destacada.id) }, Modifier.margen()) }
    if (resto.isNotEmpty()) item { GrupoConversaciones(resto, ahora, acciones.onAbrirChat, Modifier.margen().padding(top = 12.dp)) }
}

@Composable
private fun TituloLibros(estado: InicioUiState, acciones: AccionesInicio, mostrados: Int = estado.libros.size) {
    TituloSeccion("Tus libros", Modifier.margen().padding(top = 20.dp, bottom = 8.dp)) {
        if (estado.totalLibros > mostrados) {
            TextButton(onClick = acciones.onVerBiblioteca) {
                Text("Ver todos (${estado.totalLibros})", style = MaterialTheme.typography.labelLarge, color = Brand)
            }
        }
    }
}

private fun LazyListScope.librosEnCarrusel(estado: InicioUiState, acciones: AccionesInicio, ahora: Long) {
    if (estado.libros.isEmpty()) return
    item { TituloLibros(estado, acciones) }
    item {
        LazyRow(contentPadding = PaddingValues(horizontal = Margen), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(estado.libros, key = { it.id }) { libro ->
                TarjetaPortada(libro, ahora, onClick = { acciones.onAbrirLibro(libro.id) }, modifier = Modifier.width(132.dp))
            }
        }
    }
}

// Columnas sin carga perezosa para el modo ancho: tienen pocas filas.
@Composable
private fun ColumnaConversaciones(estado: InicioUiState, acciones: AccionesInicio, ahora: Long) {
    val destacada = estado.conversaciones.firstOrNull() ?: return
    TituloSeccion("Continúa donde te quedaste", Modifier.margen().padding(top = 20.dp, bottom = 8.dp))
    TarjetaContinuar(destacada, ahora, { acciones.onAbrirChat(destacada.id) }, Modifier.margen())
    GrupoConversaciones(estado.conversaciones.drop(1), ahora, acciones.onAbrirChat, Modifier.margen().padding(top = 12.dp))
}

@Composable
private fun ColumnaLibros(estado: InicioUiState, acciones: AccionesInicio, ahora: Long) {
    if (estado.libros.isEmpty()) return
    TituloLibros(estado, acciones, mostrados = minOf(6, estado.libros.size))
    estado.libros.take(6).chunked(3).forEach { fila ->
        Row(Modifier.margen().padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            fila.forEach { libro -> TarjetaPortada(libro, ahora, onClick = { acciones.onAbrirLibro(libro.id) }, modifier = Modifier.weight(1f)) }
            repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** Saludo y las dos acciones principales en un solo bloque: el ancla visual de Inicio. */
@Composable
private fun Portada(nombre: String?, acciones: AccionesInicio, alto: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(alto)
            .clip(RoundedCornerShape(32.dp))
            .background(Brand),
    ) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp)) {
            Text(if (nombre != null) "Hola," else "Hola", style = MaterialTheme.typography.titleLarge, color = Paper)
            Text(nombre ?: "¿Qué aprendemos hoy?", style = MaterialTheme.typography.displaySmall, color = Paper, modifier = Modifier.semantics { heading() })
        }
        Row(
            Modifier.align(Alignment.BottomStart).widthIn(max = 560.dp).fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AccionPortada("Pregúntale a Yachay", Icons.Outlined.Forum, Paper, Ink, acciones.onPreguntar, Modifier.weight(1.4f))
            AccionPortada("Escanear", Icons.Outlined.CameraAlt, Paper.copy(alpha = 0.18f), Paper, acciones.onEscanear, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AccionPortada(texto: String, icono: ImageVector, fondo: Color, contenido: Color, onClick: () -> Unit, modifier: Modifier) {
    val interaccion = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        color = fondo,
        shape = RoundedCornerShape(50),
        interactionSource = interaccion,
        modifier = modifier.height(56.dp).encogerAlPresionar(interaccion),
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icono, contentDescription = null, tint = if (contenido == Ink) Brand else contenido)
            Spacer(Modifier.width(8.dp))
            Text(texto, style = MaterialTheme.typography.labelLarge, color = contenido, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** La conversación más reciente, destacada: portada del libro, título grande y flecha para seguir. */
@Composable
private fun TarjetaContinuar(c: ConversacionResumen, ahora: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaccion = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        color = Paper,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 2.dp,
        interactionSource = interaccion,
        modifier = modifier.fillMaxWidth().encogerAlPresionar(interaccion),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (c.area != null) {
                PortadaLibro(c.area, Modifier.width(64.dp))
            } else {
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(BrandSoft), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Forum, contentDescription = null, tint = Brand, modifier = Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(c.libro ?: "Pregunta libre", style = MaterialTheme.typography.labelMedium, color = Brand, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(c.titulo, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(c.detalle(ahora).substringAfterLast(" · "), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(44.dp).clip(CircleShape).background(Brand), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Paper)
            }
        }
    }
}
