package pe.edu.unmsm.yachaytext.ui.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import pe.edu.unmsm.yachaytext.data.PerfilRepository
import pe.edu.unmsm.yachaytext.domain.tutor.IniciarConsulta
import pe.edu.unmsm.yachaytext.ui.biblioteca.LibroUi
import pe.edu.unmsm.yachaytext.ui.common.EstadoVacio
import pe.edu.unmsm.yachaytext.ui.common.TarjetaConversacion
import pe.edu.unmsm.yachaytext.ui.common.TarjetaLibro
import pe.edu.unmsm.yachaytext.ui.common.TituloSeccion
import pe.edu.unmsm.yachaytext.ui.common.tiempoRelativo
import pe.edu.unmsm.yachaytext.ui.theme.Ink
import pe.edu.unmsm.yachaytext.ui.theme.Mist
import pe.edu.unmsm.yachaytext.ui.theme.Paper
import pe.edu.unmsm.yachaytext.ui.theme.TextSecondary

data class ConversacionReciente(val id: Long, val titulo: String, val fechaInicio: Long)

data class InicioUiState(
    val nombre: String? = null,
    val conversaciones: List<ConversacionReciente> = emptyList(),
    val libros: List<LibroUi> = emptyList(),
    val cargando: Boolean = true,
)

private const val RECIENTES = 3

class InicioViewModel(
    perfil: PerfilRepository,
    chat: ChatRepository,
    libros: LibroRepository,
    private val iniciarConsulta: IniciarConsulta,
) : ViewModel() {

    val estado: StateFlow<InicioUiState> = combine(perfil.observar(), chat.observarConversaciones(), libros.observarLibros()) { p, c, l ->
        InicioUiState(
            nombre = p.nombre,
            conversaciones = c.take(RECIENTES).map { ConversacionReciente(it.id, it.titulo, it.fechaInicio) },
            libros = l.take(RECIENTES).map { LibroUi(it.id, it.titulo, it.areaCurso, it.numPaginas, it.fechaIngesta) },
            cargando = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioUiState())

    /** Con [libroId], la consulta queda ligada a ese libro (hasta que exista Detalle de libro). */
    fun nuevaConsulta(libroId: Long? = null, alCrear: (Long) -> Unit) {
        viewModelScope.launch { alCrear(iniciarConsulta(libroId)) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                with((this[APPLICATION_KEY] as YachayApplication).container) {
                    InicioViewModel(perfilRepository, chatRepository, libroRepository, iniciarConsulta)
                }
            }
        }
    }
}

@Composable
fun InicioRoute(
    onAbrirChat: (Long) -> Unit,
    onEscanear: () -> Unit,
    viewModel: InicioViewModel = viewModel(factory = InicioViewModel.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    InicioScreen(
        estado = estado,
        onPreguntar = { viewModel.nuevaConsulta(alCrear = onAbrirChat) },
        onEscanear = onEscanear,
        onAbrirChat = onAbrirChat,
        onAbrirLibro = { viewModel.nuevaConsulta(it, onAbrirChat) },
    )
}

@Composable
fun InicioScreen(
    estado: InicioUiState,
    onPreguntar: () -> Unit,
    onEscanear: () -> Unit,
    onAbrirChat: (Long) -> Unit,
    onAbrirLibro: (Long) -> Unit,
    ahora: Long = System.currentTimeMillis(),
) {
    Box(Modifier.fillMaxSize().background(Paper).statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Saludo(estado.nombre) }
            item { Acciones(onPreguntar, onEscanear) }

            if (!estado.cargando && estado.conversaciones.isEmpty() && estado.libros.isEmpty()) {
                item {
                    EstadoVacio(
                        icono = Icons.AutoMirrored.Outlined.LibraryBooks,
                        titulo = "Empieza con tu libro",
                        texto = "Escanea una página o hazle una pregunta a Yachay. Todo funciona sin internet.",
                        accion = "Escanear página",
                        onAccion = onEscanear,
                    )
                }
            }

            if (estado.conversaciones.isNotEmpty()) {
                item { TituloSeccion("Continúa donde te quedaste") }
                items(estado.conversaciones, key = { "c${it.id}" }) { c ->
                    TarjetaConversacion(c.titulo, tiempoRelativo(c.fechaInicio, ahora), onClick = { onAbrirChat(c.id) })
                }
            }

            if (estado.libros.isNotEmpty()) {
                item { TituloSeccion("Tus libros") }
                items(estado.libros, key = { "l${it.id}" }) { libro ->
                    TarjetaLibro(libro, ahora, onClick = { onAbrirLibro(libro.id) })
                }
            }
        }
    }
}

@Composable
private fun Saludo(nombre: String?) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Brush.linearGradient(listOf(Color(0xFFDCDCDC), Mist)), RoundedCornerShape(40.dp))
            .padding(28.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column {
            Text(if (nombre != null) "Hola," else "Hola", style = MaterialTheme.typography.titleLarge, color = Ink)
            Text(
                nombre ?: "¿Qué aprendemos hoy?",
                style = MaterialTheme.typography.displaySmall,
                color = Ink,
            )
        }
    }
}

@Composable
private fun Acciones(onPreguntar: () -> Unit, onEscanear: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            onClick = onPreguntar,
            color = Mist,
            shape = RoundedCornerShape(50),
            modifier = Modifier.weight(1f).height(72.dp),
        ) {
            Box(Modifier.padding(horizontal = 28.dp), contentAlignment = Alignment.CenterStart) {
                Text("¿Tienes alguna pregunta?", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Surface(onClick = onEscanear, color = Ink, shape = CircleShape, modifier = Modifier.size(72.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = "Escanear página", tint = Paper, modifier = Modifier.size(30.dp))
            }
        }
    }
}
