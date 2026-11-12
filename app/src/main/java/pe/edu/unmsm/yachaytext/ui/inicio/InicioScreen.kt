package pe.edu.unmsm.yachaytext.ui.inicio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import pe.edu.unmsm.yachaytext.ui.common.BotonPrincipal
import pe.edu.unmsm.yachaytext.ui.common.ConversacionResumen
import pe.edu.unmsm.yachaytext.ui.common.EstadoVacio
import pe.edu.unmsm.yachaytext.ui.common.PaisajeAndino
import pe.edu.unmsm.yachaytext.ui.common.TarjetaConversacion
import pe.edu.unmsm.yachaytext.ui.common.TarjetaLibro
import pe.edu.unmsm.yachaytext.ui.common.TituloSeccion
import pe.edu.unmsm.yachaytext.ui.common.resumir
import pe.edu.unmsm.yachaytext.ui.theme.Brand
import pe.edu.unmsm.yachaytext.ui.theme.Ink
import pe.edu.unmsm.yachaytext.ui.theme.Line
import pe.edu.unmsm.yachaytext.ui.theme.Linen
import pe.edu.unmsm.yachaytext.ui.theme.Paper

data class InicioUiState(
    val nombre: String? = null,
    val conversaciones: List<ConversacionResumen> = emptyList(),
    val libros: List<LibroUi> = emptyList(),
    val totalLibros: Int = 0,
    val cargando: Boolean = true,
)

private const val RECIENTES = 3

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
            libros = l.take(RECIENTES).map { LibroUi(it.id, it.titulo, it.areaCurso, it.numPaginas, it.fechaIngesta) },
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

@Composable
fun InicioScreen(estado: InicioUiState, acciones: AccionesInicio, ahora: Long = System.currentTimeMillis()) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Linen).statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        val relleno = PaddingValues(20.dp)
        val separacion = Arrangement.spacedBy(12.dp)
        if (maxWidth >= 840.dp) {
            // Tableta en horizontal: acciones y conversaciones a la izquierda, libros a la derecha.
            Row(Modifier.widthIn(max = 1200.dp).fillMaxSize()) {
                LazyColumn(Modifier.weight(1f).fillMaxHeight(), contentPadding = relleno, verticalArrangement = separacion) {
                    cabecera(estado, acciones)
                    conversaciones(estado, acciones, ahora)
                }
                LazyColumn(Modifier.weight(1f).fillMaxHeight(), contentPadding = relleno, verticalArrangement = separacion) {
                    libros(estado, acciones, ahora)
                }
            }
        } else {
            LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize(), contentPadding = relleno, verticalArrangement = separacion) {
                cabecera(estado, acciones)
                conversaciones(estado, acciones, ahora)
                libros(estado, acciones, ahora)
            }
        }
    }
}

private fun LazyListScope.cabecera(estado: InicioUiState, acciones: AccionesInicio) {
    item { Saludo(estado.nombre) }
    item { Acciones(acciones.onPreguntar, acciones.onEscanear) }
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
    if (estado.conversaciones.isEmpty()) return
    item { TituloSeccion("Continúa donde te quedaste") }
    items(estado.conversaciones, key = { "c${it.id}" }) { c ->
        TarjetaConversacion(c.titulo, c.detalle(ahora), onClick = { acciones.onAbrirChat(c.id) }, area = c.area)
    }
}

private fun LazyListScope.libros(estado: InicioUiState, acciones: AccionesInicio, ahora: Long) {
    if (estado.libros.isEmpty()) return
    item {
        TituloSeccion("Tus libros") {
            if (estado.totalLibros > estado.libros.size) {
                TextButton(onClick = acciones.onVerBiblioteca) {
                    Text("Ver todos (${estado.totalLibros})", style = MaterialTheme.typography.labelLarge, color = Brand)
                }
            }
        }
    }
    items(estado.libros, key = { "l${it.id}" }) { libro ->
        TarjetaLibro(libro, ahora, onClick = { acciones.onAbrirLibro(libro.id) })
    }
}

@Composable
private fun Saludo(nombre: String?) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Brand),
    ) {
        PaisajeAndino()
        Column(Modifier.align(Alignment.BottomStart).padding(28.dp)) {
            Text(if (nombre != null) "Hola," else "Hola", style = MaterialTheme.typography.titleLarge, color = Paper)
            Text(
                nombre ?: "¿Qué aprendemos hoy?",
                style = MaterialTheme.typography.displaySmall,
                color = Paper,
                modifier = Modifier.semantics { heading() },
            )
        }
    }
}

/** Las dos acciones principales, ambas con texto visible (reconocer antes que recordar). */
@Composable
private fun Acciones(onPreguntar: () -> Unit, onEscanear: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            onClick = onPreguntar,
            color = Paper,
            border = BorderStroke(1.dp, Line),
            shape = RoundedCornerShape(50),
            modifier = Modifier.weight(1f).height(64.dp),
        ) {
            Row(Modifier.padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Forum, contentDescription = null, tint = Brand)
                Spacer(Modifier.width(12.dp))
                Text("Pregúntale a Yachay", style = MaterialTheme.typography.titleMedium, color = Ink)
            }
        }
        Spacer(Modifier.width(12.dp))
        BotonPrincipal("Escanear", onEscanear, icono = Icons.Outlined.CameraAlt, alto = 64.dp)
    }
}
